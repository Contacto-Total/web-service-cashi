#!/usr/bin/env bash
# ============================================================================
# parallel_run_import_daily.sh — valida import-data (upsert+sync) e import-daily
# end-to-end sobre una SUBCARTERA DESECHABLE clonada de la 27 (foh_cas_cst).
# Crea todo, ejercita, valida por aserciones y ELIMINA todo (trap de teardown).
# Valida el motor ACTIVO del servicio (legacy o sp). Correr en el HOST.
#
#   DBPASS='<pass>' ./parallel_run_import_daily.sh
# ============================================================================
set -uo pipefail

BASE=${BASE_URL:-http://localhost:8085}
API="$BASE/api/v1/system-config/header-configurations"
DBPASS=${DBPASS:-CAMBIAR_PASSWORD}
MYSQL=(mysql -h 127.0.0.1 -u admin "-p$DBPASS" cashi_db -N -B)
Q(){ "${MYSQL[@]}" -e "$1" 2>/dev/null; }
num(){ grep -oE "\"$1\":[0-9]+" | grep -oE '[0-9]+' | head -1; }
post(){ curl -s -X POST "$1" -H 'Content-Type: application/json' -d "$2"; }

SRC_SUB=27; CARTERA=17; SUBCODE=ZZH
TI=foh_cas_zzh; TA=ini_foh_cas_zzh
SUB=""; FAIL=0

teardown(){
  echo "== TEARDOWN =="
  if [ -n "$SUB" ]; then
    Q "DELETE FROM metodos_contacto WHERE id_cliente IN (SELECT id FROM clientes WHERE id_subcartera=$SUB)"
    Q "DELETE FROM clientes WHERE id_subcartera=$SUB"
    Q "DELETE FROM configuracion_cabeceras WHERE id_subcartera=$SUB"
    Q "DELETE FROM subcarteras WHERE id=$SUB"
  fi
  Q "DROP TABLE IF EXISTS $TI"; Q "DROP TABLE IF EXISTS $TA"
  echo "   eliminado (subPortfolioId=$SUB, tablas $TI/$TA)"
}
trap teardown EXIT

echo "== SETUP: subcartera desechable (clon de $SRC_SUB) =="
Q "DELETE FROM subcarteras WHERE codigo_subcartera='$SUBCODE' AND id_cartera=$CARTERA"
Q "INSERT INTO subcarteras (codigo_subcartera,nombre_subcartera,descripcion,esta_activo,id_cartera,fecha_creacion,fecha_actualizacion) VALUES ('$SUBCODE','ZZ Harness','harness',1,$CARTERA,CURDATE(),CURDATE())"
SUB=$(Q "SELECT id FROM subcarteras WHERE codigo_subcartera='$SUBCODE' AND id_cartera=$CARTERA")
[ -n "$SUB" ] || { echo "ERROR: no se creo la subcartera"; exit 1; }
Q "INSERT INTO configuracion_cabeceras (fecha_creacion,tipo_dato,etiqueta_visual,formato,nombre_cabecera,tipo_carga,patron_regex,obligatorio,campo_origen,fecha_actualizacion,id_definicion_campo,id_subcartera,es_visible_monto,orden_monto,formato_visualizacion,auto_agregar_nuevas,columnas_ignoradas) SELECT fecha_creacion,tipo_dato,etiqueta_visual,formato,nombre_cabecera,tipo_carga,patron_regex,obligatorio,campo_origen,fecha_actualizacion,id_definicion_campo,$SUB,es_visible_monto,orden_monto,formato_visualizacion,auto_agregar_nuevas,columnas_ignoradas FROM configuracion_cabeceras WHERE id_subcartera=$SRC_SUB"
Q "DROP TABLE IF EXISTS $TI"; Q "CREATE TABLE $TI LIKE foh_cas_cst"
Q "DROP TABLE IF EXISTS $TA"; Q "CREATE TABLE $TA LIKE ini_foh_cas_cst"
echo "   subPortfolioId=$SUB  cabeceras=$(Q "SELECT COUNT(*) FROM configuracion_cabeceras WHERE id_subcartera=$SUB")  tablas=$TI/$TA"

echo "== 1) import-data INSERT (2 nuevos) =="
R=$(post "$API/subportfolio/$SUB/import-data" "{\"subPortfolioId\":$SUB,\"loadType\":\"INICIAL\",\"data\":[{\"IDENTITY_CODE\":\"ZZH001\",\"SLD_MORA_ASIG\":\"100.50\",\"DIAS_MORA_ASIG\":\"30\"},{\"IDENTITY_CODE\":\"ZZH002\",\"SLD_MORA_ASIG\":\"200.00\",\"DIAS_MORA_ASIG\":\"60\"}]}")
echo "   resp: $R"
INS=$(echo "$R"|num insertedRows); CNT=$(Q "SELECT COUNT(*) FROM $TI")
if [ "$INS" = 2 ] && [ "$CNT" = 2 ]; then echo "   OK inserted=2 filas=2"; else echo "   FALLA inserted=$INS filas=$CNT"; FAIL=1; fi

echo "== 2) import-data UPSERT (update ZZH001 + insert ZZH003) =="
R=$(post "$API/subportfolio/$SUB/import-data" "{\"subPortfolioId\":$SUB,\"loadType\":\"INICIAL\",\"data\":[{\"IDENTITY_CODE\":\"ZZH001\",\"SLD_MORA_ASIG\":\"999.99\",\"DIAS_MORA_ASIG\":\"30\"},{\"IDENTITY_CODE\":\"ZZH003\",\"SLD_MORA_ASIG\":\"300.00\",\"DIAS_MORA_ASIG\":\"90\"}]}")
echo "   resp: $R"
INS=$(echo "$R"|num insertedRows); UPD=$(echo "$R"|num updatedRows)
CNT=$(Q "SELECT COUNT(*) FROM $TI"); V=$(Q "SELECT sld_mora_asig FROM $TI WHERE identity_code='ZZH001'")
if [ "$INS" = 1 ] && [ "$UPD" = 1 ] && [ "$CNT" = 3 ] && [ "$V" = "999.99" ]; then echo "   OK ins=1 upd=1 filas=3 ZZH001.sld=999.99"; else echo "   FALLA ins=$INS upd=$UPD filas=$CNT sld=$V"; FAIL=1; fi

echo "== 3) import-daily (ACTUALIZACION + UPDATE INICIAL con COALESCE) =="
R=$(post "$API/subportfolio/$SUB/import-daily" "{\"subPortfolioId\":$SUB,\"linkField\":\"IDENTITY_CODE\",\"data\":[{\"IDENTITY_CODE\":\"ZZH001\",\"SLD_MORA\":\"555.55\",\"DIAS_MORA\":\"15\"}]}")
echo "   resp: $R"
ACNT=$(Q "SELECT COUNT(*) FROM $TA")
SLDMORA=$(Q "SELECT sld_mora FROM $TI WHERE identity_code='ZZH001'")
SLDASIG=$(Q "SELECT sld_mora_asig FROM $TI WHERE identity_code='ZZH001'")
if [ "$ACNT" -ge 1 ] && [ "$SLDMORA" = "555.55" ] && [ "$SLDASIG" = "999.99" ]; then
  echo "   OK actualizacion_filas=$ACNT  INICIAL.sld_mora=555.55  sld_mora_asig PRESERVADO=999.99"
else echo "   FALLA actualizacion=$ACNT sld_mora=$SLDMORA sld_mora_asig=$SLDASIG (esperado 555.55 / 999.99)"; FAIL=1; fi

echo "   clientes sincronizados (id_subcartera=$SUB): $(Q "SELECT COUNT(*) FROM clientes WHERE id_subcartera=$SUB")"

if [ "$FAIL" = 0 ]; then echo "== RESULTADO: PASA =="; else echo "== RESULTADO: FALLA =="; fi
exit $FAIL
