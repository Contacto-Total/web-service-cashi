#!/usr/bin/env bash
# ============================================================================
# parallel_run_edge_snapshot.sh — Validacion e2e de EDGE CASES y SNAPSHOT.
# Escenarios: regex/sourceField (DOCUMENTO=.{8}$), match por NOMBRE (identity vacio),
# fila invalida -> failedRows, y SNAPSHOT mensual (archiva a cashi_historico_db +
# trunca la tabla origen). Subcartera DESECHABLE (clon de 27). Limpia todo,
# incluyendo la tabla del historico. Correr en el HOST.  DBPASS='<pass>' ./...
# ============================================================================
set -uo pipefail
BASE=${BASE_URL:-http://localhost:8085}; API="$BASE/api/v1/system-config/header-configurations"
SNAP="$BASE/api/v1/system-config/period-snapshot"
DBPASS=${DBPASS:-CAMBIAR}; MYSQL=(mysql -h 127.0.0.1 -u admin "-p$DBPASS" cashi_db -N -B)
Q(){ "${MYSQL[@]}" -e "$1" 2>/dev/null; }
num(){ grep -oE "\"$1\":[0-9]+"|grep -oE '[0-9]+'|head -1; }
post(){ curl -s -X POST "$1" -H 'Content-Type: application/json' -d "$2"; }
SRC=27; CARTERA=17; CODE=ZZG; TI=foh_cas_zzg; TA=ini_foh_cas_zzg; SUB=""; FAIL=0
ck(){ if [ "$2" = "$3" ]; then echo "   OK  $1 ($2)"; else echo "   FALLA $1: esperado '$3' got '$2'"; FAIL=1; fi; }

teardown(){ echo "== TEARDOWN ==";
  if [ -n "$SUB" ]; then
    Q "DELETE FROM metodos_contacto WHERE id_cliente IN (SELECT id FROM clientes WHERE id_subcartera=$SUB)"
    Q "DELETE FROM clientes WHERE id_subcartera=$SUB"
    Q "DELETE FROM configuracion_cabeceras WHERE id_subcartera=$SUB"
    Q "DELETE FROM subcarteras WHERE id=$SUB"; fi
  Q "DROP TABLE IF EXISTS $TI"; Q "DROP TABLE IF EXISTS $TA"
  for h in $(Q "SELECT table_name FROM information_schema.tables WHERE table_schema='cashi_historico_db' AND table_name LIKE '${TI}\_%'"); do Q "DROP TABLE IF EXISTS cashi_historico_db.$h"; echo "   drop historico.$h"; done
  echo "   eliminado sub=$SUB"; }
trap teardown EXIT

echo "== SETUP (clon de $SRC) =="
Q "DELETE FROM subcarteras WHERE codigo_subcartera='$CODE' AND id_cartera=$CARTERA"
Q "INSERT INTO subcarteras (codigo_subcartera,nombre_subcartera,descripcion,esta_activo,id_cartera,fecha_creacion,fecha_actualizacion) VALUES ('$CODE','ZZ Edge','edge',1,$CARTERA,CURDATE(),CURDATE())"
SUB=$(Q "SELECT id FROM subcarteras WHERE codigo_subcartera='$CODE' AND id_cartera=$CARTERA")
[ -n "$SUB" ] || { echo "ERROR setup"; exit 1; }
Q "INSERT INTO configuracion_cabeceras (fecha_creacion,tipo_dato,etiqueta_visual,formato,nombre_cabecera,tipo_carga,patron_regex,obligatorio,campo_origen,fecha_actualizacion,id_definicion_campo,id_subcartera,es_visible_monto,orden_monto,formato_visualizacion,auto_agregar_nuevas,columnas_ignoradas) SELECT fecha_creacion,tipo_dato,etiqueta_visual,formato,nombre_cabecera,tipo_carga,patron_regex,obligatorio,campo_origen,fecha_actualizacion,id_definicion_campo,$SUB,es_visible_monto,orden_monto,formato_visualizacion,auto_agregar_nuevas,columnas_ignoradas FROM configuracion_cabeceras WHERE id_subcartera=$SRC"
Q "DROP TABLE IF EXISTS $TI"; Q "CREATE TABLE $TI LIKE foh_cas_cst"
Q "DROP TABLE IF EXISTS $TA"; Q "CREATE TABLE $TA LIKE ini_foh_cas_cst"
echo "   sub=$SUB"

echo "== E1) regex/sourceField: DOCUMENTO = ultimos 8 de NUM_DOCUM_IDE (.{8}\$) =="
post "$API/subportfolio/$SUB/import-data" "{\"subPortfolioId\":$SUB,\"loadType\":\"INICIAL\",\"data\":[{\"IDENTITY_CODE\":\"ZZG001\",\"NUM_DOCUM_IDE\":\"XYZ40684786\"}]}" >/dev/null
ck "documento regex (=40684786)" "$(Q "SELECT documento FROM $TI WHERE identity_code='ZZG001'")" "40684786"

echo "== E2) match por NOMBRE (identity vacio -> actualiza, no inserta) =="
post "$API/subportfolio/$SUB/import-data" "{\"subPortfolioId\":$SUB,\"loadType\":\"INICIAL\",\"data\":[{\"IDENTITY_CODE\":\"ZZG002\",\"NOMBRE_COMPLETO\":\"UNICO NOMBRE Z\",\"SLD_MORA_ASIG\":\"100\"}]}" >/dev/null
BEFORE=$(Q "SELECT COUNT(*) FROM $TI")
R=$(post "$API/subportfolio/$SUB/import-data" "{\"subPortfolioId\":$SUB,\"loadType\":\"INICIAL\",\"data\":[{\"IDENTITY_CODE\":\"\",\"NOMBRE_COMPLETO\":\"UNICO NOMBRE Z\",\"SLD_MORA_ASIG\":\"777\"}]}")
echo "   resp: $R"
ck "match-nombre updated=1" "$(echo "$R"|num updatedRows)" 1
ck "no se inserto (filas igual)" "$(Q "SELECT COUNT(*) FROM $TI")" "$BEFORE"
ck "sld actualizado=777" "$(Q "SELECT sld_mora_asig FROM $TI WHERE nombre_completo='UNICO NOMBRE Z'")" "777.00"

echo "== E3) numerico invalido ('abc'): validator LENIENT (coerciona a NULL, fila entra) =="
echo "   [comportamiento existente, identico legacy/SP: el SP path lo maneja igual]"
R=$(post "$API/subportfolio/$SUB/import-data" "{\"subPortfolioId\":$SUB,\"loadType\":\"INICIAL\",\"data\":[{\"IDENTITY_CODE\":\"ZZG010\",\"SLD_MORA_ASIG\":\"500\"},{\"IDENTITY_CODE\":\"ZZG011\",\"SLD_MORA_ASIG\":\"abc\"}]}")
echo "   resp: $R"
ck "ambas insertadas (lenient)" "$(echo "$R"|num insertedRows)" 2
ck "failedRows=0 (lenient)" "$(echo "$R"|num failedRows)" 0
ck "ZZG011.sld coercionado a NULL" "$(Q "SELECT IFNULL(sld_mora_asig,'NULL') FROM $TI WHERE identity_code='ZZG011'")" "NULL"

echo "== S) SNAPSHOT mensual: archiva a historico + trunca origen =="
CNT=$(Q "SELECT COUNT(*) FROM $TI")
echo "   filas antes del snapshot: $CNT"
STAT=$(curl -s "$SNAP/subportfolio/$SUB/status"); echo "   status: $STAT"
ck "requiresConfirmation=true" "$(echo "$STAT"|grep -o '"requiresConfirmation":[a-z]*'|cut -d: -f2)" "true"
RS=$(post "$SNAP/subportfolio/$SUB/execute" "{}"); echo "   execute: $RS"
ck "snapshot success=true" "$(echo "$RS"|grep -o '"success":[a-z]*'|cut -d: -f2)" "true"
ck "origen truncado (0 filas)" "$(Q "SELECT COUNT(*) FROM $TI")" 0
HT=$(Q "SELECT table_name FROM information_schema.tables WHERE table_schema='cashi_historico_db' AND table_name LIKE '${TI}\_%' ORDER BY table_name DESC LIMIT 1")
echo "   tabla historico: $HT"
if [ -n "$HT" ]; then ck "historico tiene las filas archivadas" "$(Q "SELECT COUNT(*) FROM cashi_historico_db.$HT")" "$CNT"; else echo "   FALLA no se creo tabla historico"; FAIL=1; fi

echo ""
if [ "$FAIL" = 0 ]; then echo "===== EDGE+SNAPSHOT: PASA ====="; else echo "===== EDGE+SNAPSHOT: FALLA ====="; fi
exit $FAIL
