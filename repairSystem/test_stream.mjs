import { $fetch } from 'ofetch';
async function run() {
  try {
    const res = await $fetch.raw('https://repair-auto.onrender.com/api/v1/me/avatar', {
      responseType: 'stream'
    });
    console.log("res._data is stream:", !!res._data?.getReader || !!res._data?.on);
    console.log("res.body is stream:", !!res.body?.getReader || !!res.body?.on);
  } catch (err) {
    console.error("Error:", err.status);
    console.error("Error body:", !!err.response?.body, !!err.response?._data);
  }
}
run();
