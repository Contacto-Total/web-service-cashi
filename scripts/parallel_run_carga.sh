#!/usr/bin/env bash
# ============================================================================
# parallel_run_carga.sh — Harness de validación end-to-end de la Carga Consolidada
#
# Valida el motor de importación ACTIVO en el servicio vivo (legacy o sp) por
# ASERCIONES DE CORRECTITUD (no por paridad con legacy: el legacy es deficiente
# en partes; el SP debe ser correcto). Reversible: captura el estado real,
# llama a la API, verifica y RESTAURA.
#
# Usa el endpoint update-complementary porque ejercita sp_import_update
# (COALESCE/colación/tipos) y NO dispara la sincronización de clientes.
#
# Cómo usar (en el HOST del servicio, donde localhost:8085 responde):
#   1) Desplegado con app.import.engine=legacy  ->  ./parallel_run_carga.sh   (baseline)
#   2) Redesplegar con app.import.engine=sp      ->  ./parallel_run_carga.sh   (debe pasar igual + más rápido)
#
# Variables (override por entorno):
#   BASE_URL (http://localhost:8085)  SUB (27=foh_cas_cst)  TABLE (foh_cas_cst)
#   COL (score_final)  LINK (identity_code)  DBPASS (password de MySQL admin)
# ============================================================================
set -uo pipefail

BASE_URL=${BASE_URL:-http://localhost:8085}
SUB=${SUB:-27}
TABLE=${TABLE:-foh_cas_cst}
COL=${COL:-score_final}
LINK=${LINK:-identity_code}
DBHOST=${DBHOST:-127.0.0.1}
DBUSER=${DBUSER:-admin}
DBPASS=${DBPASS:-CAMBIAR_PASSWORD}
DB=${DB:-cashi_db}

MYSQL=(mysql -h "$DBHOST" -u "$DBUSER" "-p$DBPASS" "$DB" -N -B)
ENDPOINT="$BASE_URL/api/v1/system-config/header-configurations/subportfolio/$SUB/update-complementary"
TESTVAL="PR_$(date +%s)"

echo "== Harness carga consolidada =="
echo "   endpoint=$ENDPOINT  tabla=$TABLE  col=$COL  link=$LINK  testval=$TESTVAL"

# 1) Elegir 3 identity_codes reales existentes
mapfile -t ICS < <("${MYSQL[@]}" -e \
  "SELECT \`$LINK\` FROM \`$TABLE\` WHERE \`$LINK\` IS NOT NULL AND \`$LINK\` <> '' ORDER BY id LIMIT 3")
if [ "${#ICS[@]}" -eq 0 ]; then echo "ERROR: no hay $LINK en $TABLE"; exit 1; fi
echo "   muestras: ${ICS[*]}"

# 2) Capturar valores ANTES (para restaurar)
declare -A BEFORE
for ic in "${ICS[@]}"; do
  BEFORE[$ic]=$("${MYSQL[@]}" -e "SELECT IFNULL(\`$COL\`,'<NULL>') FROM \`$TABLE\` WHERE \`$LINK\`='$ic' LIMIT 1")
done

# 3) Construir payload (incluye 1 link inexistente -> debe contar como notFound)
ROWS=""
for ic in "${ICS[@]}"; do ROWS+="{\"$LINK\":\"$ic\",\"$COL\":\"$TESTVAL\"},"; done
ROWS+="{\"$LINK\":\"ZZ_NOEXISTE_PR\",\"$COL\":\"$TESTVAL\"}"
PAYLOAD="{\"subPortfolioId\":$SUB,\"loadType\":\"INICIAL\",\"linkField\":\"$LINK\",\"data\":[$ROWS]}"

# 4) POST a la API viva (con tiempo)
echo "== POST update-complementary =="
RESP=$(curl -s -w "\n__HTTP__%{http_code}__T__%{time_total}s" -X POST "$ENDPOINT" \
  -H 'Content-Type: application/json' -d "$PAYLOAD")
echo "   respuesta: ${RESP%%__HTTP__*}"
echo "   http/tiempo: ${RESP##*__HTTP__}"

# 5) Aserciones de correctitud
echo "== Verificación =="
FAIL=0
for ic in "${ICS[@]}"; do
  AFTER=$("${MYSQL[@]}" -e "SELECT \`$COL\` FROM \`$TABLE\` WHERE \`$LINK\`='$ic'")
  if [ "$AFTER" = "$TESTVAL" ]; then echo "   OK   $ic -> $COL='$AFTER'"; else echo "   FALLA $ic -> esperado '$TESTVAL', got '$AFTER'"; FAIL=1; fi
done
echo "   (esperado en respuesta: updatedRows=3, notFoundRows=1)"

# 6) RESTAURAR estado original
echo "== Restaurar =="
for ic in "${ICS[@]}"; do
  v=${BEFORE[$ic]}
  if [ "$v" = "<NULL>" ]; then
    "${MYSQL[@]}" -e "UPDATE \`$TABLE\` SET \`$COL\`=NULL WHERE \`$LINK\`='$ic'"
  else
    vesc=${v//\'/\'\'}
    "${MYSQL[@]}" -e "UPDATE \`$TABLE\` SET \`$COL\`='$vesc' WHERE \`$LINK\`='$ic'"
  fi
done
echo "   valores originales restaurados."

[ "$FAIL" -eq 0 ] && echo "== RESULTADO: PASA ==" || { echo "== RESULTADO: FALLA (revisar wiring SP) =="; exit 2; }
