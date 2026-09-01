import { $fetch } from 'ofetch'

async function run() {
  try {
    const res = await $fetch.raw('http://localhost:8080/api/v1/me/avatar', {
      responseType: 'arrayBuffer'
    })
    console.log('ArrayBuffer length:', res._data ? res._data.byteLength : 'undefined')
  } catch (err) {
    console.error(err.status, err.message)
  }
}
run()
