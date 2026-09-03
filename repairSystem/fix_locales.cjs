const fs = require('fs');

function addKey(file, searchKey, addKey, text) {
  let code = fs.readFileSync(file, 'utf8');
  if (!code.includes("'" + addKey + "':")) {
    code = code.replace(
      "'" + searchKey + "':",
      "'" + addKey + "': '" + text + "',\n  '" + searchKey + "':"
    );
    fs.writeFileSync(file, code);
  }
}

addKey('app/locales/ru.ts', 'dashboard', 'chats', 'Чаты');
addKey('app/locales/uz.ts', 'dashboard', 'chats', 'Chatlar');
addKey('app/locales/en.ts', 'dashboard', 'chats', 'Chats');

