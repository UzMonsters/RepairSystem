const fs = require('fs')

let code = fs.readFileSync('app/pages/admin/requests/[id].vue', 'utf8')

// 1. Re-apply t('notSpecified')
code = code.replace(/\|\|\s*'-'/g, '|| t(\'notSpecified\')')

// 2. Re-apply scoped style
const styleBlock = `
<style scoped>
[data-bs-theme="dark"] .nav-tabs .nav-link {
  color: #adb5bd;
}
[data-bs-theme="dark"] .nav-tabs .nav-link:hover,
[data-bs-theme="dark"] .nav-tabs .nav-link.active {
  color: #ffffff !important;
}
</style>
`
if (!code.includes('<style scoped>')) {
  code += '\n' + styleBlock
}

// 3. Move Actions to a Tab, but preserve EXACT logic.
const tabNavSearch = `              <li class="nav-item">
                <button type="button" class="nav-link" :class="{ active: activeTab === 'history' }" @click="activeTab = 'history'">
                  {{ t('history') || 'История' }}
                </button>
              </li>`
const tabNavReplace = tabNavSearch + `
              <li class="nav-item">
                <button type="button" class="nav-link text-warning" :class="{ active: activeTab === 'actions' }" @click="activeTab = 'actions'">
                  <i class="bi bi-magic me-1"></i>{{ t('actions') }}
                </button>
              </li>`
code = code.replace(tabNavSearch, tabNavReplace)

// The original Actions card in the right column looks like this:
const oldCardRegex = /<div class="card mb-4">\s*<div class="card-header">\s*<h3 class="card-title">\s*\{\{ t\('actions'\) \}\}\s*<\/h3>\s*<\/div>\s*<div class="card-body d-flex flex-column gap-2">([\s\S]*?)<\/div>\s*<\/div>/

const match = code.match(oldCardRegex)
if (match) {
  code = code.replace(oldCardRegex, '') // Remove from right column

  // Transform the buttons inside into a grid
  const newActionsContent = `
            <div v-show="activeTab === 'actions'">
              <div class="card mb-4">
                <div class="card-header">
                  <h3 class="card-title"><i class="bi bi-grid me-2"></i>{{ t('actions') }}</h3>
                </div>
                <div class="card-body">
                  <div class="row g-3">
                    <div class="col-md-6 col-lg-4">
                      <button
                        type="button"
                        class="btn btn-outline-danger w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        :disabled="deletingRequest"
                        @click="showModal('request-delete-modal')"
                      >
                        <i class="bi bi-trash fs-2 mb-2"></i>
                        <span class="fw-semibold">{{ t('deleteRequest') }}</span>
                      </button>
                    </div>

                    <div v-if="can('diagnosis')" class="col-md-6 col-lg-4">
                      <button
                        type="button"
                        class="btn btn-outline-info w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        @click="openExecModal('diagnosis')"
                      >
                        <i class="bi bi-search fs-2 mb-2"></i>
                        <span class="fw-semibold">{{ t('diagnosis') }}</span>
                      </button>
                    </div>

                    <div v-if="can('start')" class="col-md-6 col-lg-4">
                      <button
                        type="button"
                        class="btn btn-primary w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        @click="openExecModal('start')"
                      >
                        <i class="bi bi-play-circle fs-2 mb-2"></i>
                        <span class="fw-semibold">{{ t('start') }}</span>
                      </button>
                    </div>

                    <div v-if="can('wait-for-parts')" class="col-md-6 col-lg-4">
                      <button
                        type="button"
                        class="btn btn-outline-warning w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        @click="openExecModal('wait-for-parts')"
                      >
                        <i class="bi bi-box-seam fs-2 mb-2"></i>
                        <span class="fw-semibold">{{ t('waitForParts') }}</span>
                      </button>
                    </div>

                    <div v-if="can('resume')" class="col-md-6 col-lg-4">
                      <button
                        type="button"
                        class="btn btn-primary w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        @click="openExecModal('resume')"
                      >
                        <i class="bi bi-play-circle fs-2 mb-2"></i>
                        <span class="fw-semibold">{{ t('resume') }}</span>
                      </button>
                    </div>

                    <div v-if="can('complete')" class="col-md-6 col-lg-4">
                      <button
                        type="button"
                        class="btn btn-success w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        @click="openExecModal('complete')"
                      >
                        <i class="bi bi-check2-circle fs-2 mb-2"></i>
                        <span class="fw-semibold">{{ t('complete') }}</span>
                      </button>
                    </div>

                    <div v-if="can('cancel')" class="col-md-6 col-lg-4">
                      <button
                        type="button"
                        class="btn btn-outline-danger w-100 h-100 d-flex flex-column align-items-center justify-content-center p-4 shadow-sm"
                        @click="openExecModal('cancel')"
                      >
                        <i class="bi bi-x-circle fs-2 mb-2"></i>
                        <span class="fw-semibold">{{ t('cancelRequest') }}</span>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
`

  code = code.replace(
    '</div>\n\n          <div class="col-lg-4">',
    newActionsContent + '\n          </div>\n\n          <div class="col-lg-4">'
  )
}

fs.writeFileSync('app/pages/admin/requests/[id].vue', code)
