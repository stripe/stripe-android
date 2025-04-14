# Shared variables used in our localization scripts

MODULES=(
  "link"
  "paymentsheet"
  "payments-core"
  "payments-ui-core"
  "stripe-ui-core"
  "stripe-core"
  "identity"
  "financial-connections"
  "stripecardscan"
)

#
# Take great caution here, there are languages that are and are not finalized that may or may
# not contain all the required translated strings.
LANGUAGES="bg_BG,ca-ES,zh-Hans,zh-HK,zh-Hant,hr,cs-CZ,da,nl,en-GB,en,et-EE,fil,fi,fr-CA,fr,de,el-GR,hu,id,it,ja,ko,lt-LT,lv-LV,ms-MY,mt,nb,no,nn-NO,pl-PL,pt-BR,pt-PT,ro-RO,ru,sk-SK,sl-SI,es-419,es,sv,th,tr,vi"

# API token should be retrieved from: https://app.lokalise.com/profile#apitokens
# Set it as your environment variable with name "LOKALISE_API_TOKEN"
API_TOKEN=$LOKALISE_API_TOKEN
PROJECT_ID=747824695e51bc2f4aa912.89576472
