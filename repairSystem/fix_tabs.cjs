const fs = require('fs')

let code = fs.readFileSync('app/pages/admin/requests/[id].vue', 'utf8')

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
            </div>\n\n          </div>\n\n          <div class="col-lg-4">`

code = code.replace(/<\/div>[\s]*<div class="col-lg-4">/, newActionsContent)

fs.writeFileSync('app/pages/admin/requests/[id].vue', code)
