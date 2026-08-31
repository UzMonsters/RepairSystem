const fs = require('fs');
const path = 'app/pages/admin/customers/index.vue';
let content = fs.readFileSync(path, 'utf8');
content = content.replace(
  /{ label: t\('telegramLinked'\), field: 'registrationSource' }/g,
  "{ label: t('binding'), field: 'registrationSource' }"
);
const regex = /<td>\s*<span\s*v-if="c\.telegramLinked"\s*class="status-chip status-completed"\s*>\s*<span class="status-dot" \/>TG\s*<\/span>\s*<span v-else>-<\/span>\s*<\/td>/s;
const replacement = `<td>
                <i v-if="c.registrationSource === 'TELEGRAM' || c.telegramLinked" class="bi bi-telegram text-primary fs-5" :title="t('telegramLinked')"></i>
                <i v-else-if="c.registrationSource === 'GOOGLE'" class="bi bi-google text-danger fs-5" title="Google"></i>
                <i v-else-if="c.registrationSource === 'PHONE'" class="bi bi-telephone text-success fs-5" :title="t('phone')"></i>
                <span v-else>-</span>
              </td>`;
content = content.replace(regex, replacement);
fs.writeFileSync(path, content);
