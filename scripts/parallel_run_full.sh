#!/usr/bin/env bash
# ============================================================================
# parallel_run_full.sh — Validacion integral e2e de la Carga Consolidada.
# Cubre todas las tablas (INICIAL, ACTUALIZACION, clientes, metodos_contacto)
# y escenarios: import-data insert/upsert/match-nombre, sync con telefonos,
# PRESERVACION de estado_osiptel/whatsapp/telefono_extra (fix F2.5-a),
# import-daily (COALESCE), update-complementary (not-found).
# Sobre subcartera DESECHABLE (clon de la 27). Crea/ejercita/elimina todo.
# Correr en el HOST.   DBPASS='<pass>' ./parallel_run_full.sh
# ============================================================================
set -uo pipefail
BASE=${BASE_URL:-http://localhost:8085}; API="$BASE/api/v1/system-config/header-configurations"
DBPASS=${DBPASS:-CAMBIAR}; MYSQL=(mysql -h 127.0.0.1 -u admin "-p$DBPASS" cashi_db -N -B)
Q(){ "${MYSQL[@]}" -e "$1" 2>/dev/null; }
num(){ grep -oE "\"$1\":[0-9]+"|grep -oE '[0-9]+'|head -1; }
post(){ curl -s -X POST "$1" -H 'Content-Type: application/json' -d "$2"; }
SRC=27; CARTERA=17; CODE=ZZF; TI=foh_cas_zzf; TA=ini_foh_cas_zzf; SUB=""; FAIL=0
ck(){ if [ "$2" = "$3" ]; then echo "   OK  $1 ($2)"; else echo "   FALLA $1: esperado '$3' got '$2'"; FAIL=1; fi; }
cknz(){ if [ "$2" -ge "$3" ] 2>/dev/null; then echo "   OK  $1 ($2>=$3)"; else echo "   FALLA $1: esperado >=$3 got '$2'"; FAIL=1; fi; }

teardown(){ echo "== TEARDOWN ==";
  if [ -n "$SUB" ]; then
    Q "DELETE FROM metodos_contacto WHERE id_cliente IN (SELECT id FROM clientes WHERE id_subcartera=$SUB)"
    Q "DELETE FROM clientes WHERE id_subcartera=$SUB"
    Q "DELETE FROM configuracion_cabeceras WHERE id_subcartera=$SUB"
    Q "DELETE FROM subcarteras WHERE id=$SUB"; fi
  Q "DROP TABLE IF EXISTS $TI"; Q "DROP TABLE IF EXISTS $TA"; echo "   eliminado sub=$SUB"; }
trap teardown EXIT

echo "== SETUP: subcartera desechable (clon de $SRC) =="
Q "DELETE FROM subcarteras WHERE codigo_subcartera='$CODE' AND id_cartera=$CARTERA"
Q "INSERT INTO subcarteras (codigo_subcartera,nombre_subcartera,descripcion,esta_activo,id_cartera,fecha_creacion,fecha_actualizacion) VALUES ('$CODE','ZZ Full','full',1,$CARTERA,CURDATE(),CURDATE())"
SUB=$(Q "SELECT id FROM subcarteras WHERE codigo_subcartera='$CODE' AND id_cartera=$CARTERA")
[ -n "$SUB" ] || { echo "ERROR setup"; exit 1; }
Q "INSERT INTO configuracion_cabeceras (fecha_creacion,tipo_dato,etiqueta_visual,formato,nombre_cabecera,tipo_carga,patron_regex,obligatorio,campo_origen,fecha_actualizacion,id_definicion_campo,id_subcartera,es_visible_monto,orden_monto,formato_visualizacion,auto_agregar_nuevas,columnas_ignoradas) SELECT fecha_creacion,tipo_dato,etiqueta_visual,formato,nombre_cabecera,tipo_carga,patron_regex,obligatorio,campo_origen,fecha_actualizacion,id_definicion_campo,$SUB,es_visible_monto,orden_monto,formato_visualizacion,auto_agregar_nuevas,columnas_ignoradas FROM configuracion_cabeceras WHERE id_subcartera=$SRC"
Q "DROP TABLE IF EXISTS $TI"; Q "CREATE TABLE $TI LIKE foh_cas_cst"
Q "DROP TABLE IF EXISTS $TA"; Q "CREATE TABLE $TA LIKE ini_foh_cas_cst"
echo "   sub=$SUB tablas=$TI/$TA cabeceras=$(Q "SELECT COUNT(*) FROM configuracion_cabeceras WHERE id_subcartera=$SUB")"

ROW(){ echo "{\"IDENTITY_CODE\":\"$1\",\"NUM_DOCUM_IDE\":\"$2\",\"NOMBRE_COMPLETO\":\"$3\",\"TELEFONO_CELULAR\":\"$4\",\"EMAIL\":\"$5\",\"SLD_MORA_ASIG\":\"$6\"}"; }

echo "== A1) import-data INSERT + sync (clientes + metodos_contacto) =="
D="$(ROW ZZF001 11111111 'Juan Perez' 999111001 j1@t.com 100),$(ROW ZZF002 22222222 'Maria Lopez' 999111002 j2@t.com 200)"
R=$(post "$API/subportfolio/$SUB/import-data" "{\"subPortfolioId\":$SUB,\"loadType\":\"INICIAL\",\"data\":[$D]}")
echo "   resp: $R"
ck "inserted" "$(echo "$R"|num insertedRows)" 2
ck "filas INICIAL" "$(Q "SELECT COUNT(*) FROM $TI")" 2
cknz "clientes" "$(Q "SELECT COUNT(*) FROM clientes WHERE id_subcartera=$SUB")" 2
cknz "metodos_contacto (tel/email)" "$(Q "SELECT COUNT(*) FROM metodos_contacto m JOIN clientes c ON c.id=m.id_cliente WHERE c.id_subcartera=$SUB")" 2

echo "== B1/B2) PRESERVACION estado_osiptel/whatsapp al reimportar (fix F2.5-a) =="
Q "UPDATE metodos_contacto m JOIN clientes c ON c.id=m.id_cliente SET m.estado_osiptel='PERTENECE', m.estado_whatsapp='TIENE' WHERE c.id_subcartera=$SUB"
R=$(post "$API/subportfolio/$SUB/import-data" "{\"subPortfolioId\":$SUB,\"loadType\":\"INICIAL\",\"data\":[$D]}")
ck "estado_osiptel preservado (sin SIN_VALIDAR)" "$(Q "SELECT COUNT(*) FROM metodos_contacto m JOIN clientes c ON c.id=m.id_cliente WHERE c.id_subcartera=$SUB AND m.estado_osiptel='SIN_VALIDAR'")" 0
cknz "estado_osiptel=PERTENECE tras resync" "$(Q "SELECT COUNT(*) FROM metodos_contacto m JOIN clientes c ON c.id=m.id_cliente WHERE c.id_subcartera=$SUB AND m.estado_osiptel='PERTENECE'")" 1
cknz "estado_whatsapp=TIENE tras resync" "$(Q "SELECT COUNT(*) FROM metodos_contacto m JOIN clientes c ON c.id=m.id_cliente WHERE c.id_subcartera=$SUB AND m.estado_whatsapp='TIENE'")" 1

echo "== B3) PRESERVACION telefono_extra al reimportar =="
CID=$(Q "SELECT id FROM clientes WHERE id_subcartera=$SUB ORDER BY id LIMIT 1")
Q "INSERT INTO metodos_contacto (id_cliente,tipo_contacto,subtipo,valor,etiqueta,fecha_importacion,estado,estado_osiptel,estado_whatsapp,estado_contactabilidad) VALUES ($CID,'telefono','telefono_extra','988777666','telefono_extra',CURDATE(),'ACTIVE','PERTENECE','SIN_VALIDAR','NUEVO')"
post "$API/subportfolio/$SUB/import-data" "{\"subPortfolioId\":$SUB,\"loadType\":\"INICIAL\",\"data\":[$D]}" >/dev/null
ck "telefono_extra sobrevive resync" "$(Q "SELECT COUNT(*) FROM metodos_contacto WHERE id_cliente=$CID AND subtipo='telefono_extra' AND valor='988777666'")" 1

echo "== A2) import-data UPSERT (update ZZF001 + insert ZZF003) =="
D2="$(ROW ZZF001 11111111 'Juan Perez' 999111001 j1@t.com 999.99),$(ROW ZZF003 33333333 'Pedro Ruiz' 999111003 j3@t.com 300)"
R=$(post "$API/subportfolio/$SUB/import-data" "{\"subPortfolioId\":$SUB,\"loadType\":\"INICIAL\",\"data\":[$D2]}")
echo "   resp: $R"
ck "ins=1" "$(echo "$R"|num insertedRows)" 1
ck "upd=1" "$(echo "$R"|num updatedRows)" 1
ck "filas=3" "$(Q "SELECT COUNT(*) FROM $TI")" 3
ck "ZZF001.sld=999.99" "$(Q "SELECT sld_mora_asig FROM $TI WHERE identity_code='ZZF001'")" 999.99

echo "== C) import-daily (ACTUALIZACION + INICIAL COALESCE) =="
R=$(post "$API/subportfolio/$SUB/import-daily" "{\"subPortfolioId\":$SUB,\"linkField\":\"IDENTITY_CODE\",\"data\":[{\"IDENTITY_CODE\":\"ZZF001\",\"SLD_MORA\":\"555.55\",\"DIAS_MORA\":\"15\"}]}")
echo "   resp: $R"
cknz "ACTUALIZACION filas" "$(Q "SELECT COUNT(*) FROM $TA")" 1
ck "INICIAL.sld_mora=555.55" "$(Q "SELECT sld_mora FROM $TI WHERE identity_code='ZZF001'")" 555.55
ck "sld_mora_asig PRESERVADO=999.99 (COALESCE)" "$(Q "SELECT sld_mora_asig FROM $TI WHERE identity_code='ZZF001'")" 999.99

echo "== D) update-complementary (update + link inexistente) =="
R=$(post "$API/subportfolio/$SUB/update-complementary" "{\"subPortfolioId\":$SUB,\"loadType\":\"INICIAL\",\"linkField\":\"IDENTITY_CODE\",\"data\":[{\"IDENTITY_CODE\":\"ZZF002\",\"SLD_MORA_ASIG\":\"888.88\"},{\"IDENTITY_CODE\":\"NOEXISTE\",\"SLD_MORA_ASIG\":\"1\"}]}")
echo "   resp: $R"
ck "comp updated=1" "$(echo "$R"|num updatedRows)" 1
ck "comp notFound=1" "$(echo "$R"|num notFoundRows)" 1
ck "ZZF002.sld=888.88" "$(Q "SELECT sld_mora_asig FROM $TI WHERE identity_code='ZZF002'")" 888.88

echo ""
if [ "$FAIL" = 0 ]; then echo "===== RESULTADO GLOBAL: PASA ====="; else echo "===== RESULTADO GLOBAL: FALLA ====="; fi
exit $FAIL
