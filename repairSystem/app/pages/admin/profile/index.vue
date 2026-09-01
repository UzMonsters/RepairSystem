<script setup lang="ts">
import { getApiErrorMessage } from '~/utils/api'

const { user, updateProfile, uploadAvatar, deleteAvatar, changePassword, logout, logoutAll, avatarObjectUrl } = useAuth()
const { t } = useLocale()

const loadingProfile = ref(false)
const loadingAvatar = ref(false)
const loadingPassword = ref(false)

const profileForm = ref({
  fullName: user.value?.fullName || '',
  phone: user.value?.phone || ''
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const avatarPreviewUrl = ref<string | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const avatarSrc = computed(() => avatarPreviewUrl.value || avatarObjectUrl.value || undefined)
const profileInitials = computed(() => (user.value?.fullName || 'Administrator')
  .split(/\s+/)
  .filter(Boolean)
  .map(part => part[0])
  .join('')
  .slice(0, 2)
  .toUpperCase())

async function handleFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  avatarPreviewUrl.value = URL.createObjectURL(file)
  loadingAvatar.value = true
  try {
    await uploadAvatar(file)
    avatarPreviewUrl.value = null
    if (fileInput.value) fileInput.value.value = ''
    useToast().showSuccess(t('savedSuccessfully'))
  } catch (error) {
    avatarPreviewUrl.value = null
    useToast().showError(getApiErrorMessage(error, 'Failed to upload avatar'))
  } finally {
    loadingAvatar.value = false
  }
}

async function handleProfileSubmit() {
  loadingProfile.value = true
  try {
    await updateProfile(profileForm.value)
    useToast().showSuccess(t('savedSuccessfully'))
  } catch (e) {
    useToast().showError(getApiErrorMessage(e, 'Failed to update profile'))
  } finally {
    loadingProfile.value = false
  }
}

async function handleAvatarDelete() {
  loadingAvatar.value = true
  try {
    await deleteAvatar()
    useToast().showSuccess(t('deletedSuccessfully'))
  } catch (e) {
    useToast().showError(getApiErrorMessage(e, 'Failed to delete avatar'))
  } finally {
    loadingAvatar.value = false
  }
}

async function handlePasswordSubmit() {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    useToast().showError(t('passwordMismatch') || 'Passwords do not match')
    return
  }
  loadingPassword.value = true
  try {
    await changePassword(passwordForm.value)
    useToast().showSuccess(t('savedSuccessfully'))
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    setTimeout(() => {
      logout()
    }, 2000)
  } catch (e) {
    useToast().showError(getApiErrorMessage(e, 'Failed to change password'))
  } finally {
    loadingPassword.value = false
  }
}
</script>

<template>
  <AppContent
    :title="t('profile') || 'Profile'"
    :breadcrumbs="[{ label: t('home'), to: '/admin' }, { label: t('profile') || 'Profile' }]"
  >
    <div class="row g-4">
      <!-- Left Column: Avatar & Profile -->
      <div class="col-lg-4">
        <div class="card mb-4 border-0 shadow-sm">
          <div class="card-body text-center pt-4 pb-4">
            <div
              class="profile-avatar-editor mx-auto mb-3"
              style="width: fit-content;"
            >
              <div
                class="profile-avatar-frame mx-auto"
                :class="{ 'has-avatar': avatarPreviewUrl || avatarObjectUrl }"
              >
                <img
                  v-if="avatarPreviewUrl || avatarObjectUrl"
                  :src="avatarSrc"
                  alt="Avatar"
                  class="rounded-circle shadow object-fit-cover"
                >
                <div
                  v-else
                  class="d-flex align-items-center justify-content-center rounded-circle bg-primary text-white shadow h-100 w-100"
                >
                  <span class="fw-bold fs-2">{{ profileInitials }}</span>
                </div>
                <label
                  class="profile-avatar-action profile-avatar-upload"
                  :class="{ disabled: loadingAvatar }"
                  :title="t('chooseFile')"
                >
                  <i
                    v-if="!loadingAvatar"
                    class="bi bi-camera-fill profile-avatar-camera"
                  />
                  <span
                    v-else
                    class="spinner-border spinner-border-sm"
                  />
                  <input
                    ref="fileInput"
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    :disabled="loadingAvatar"
                    @change="handleFileSelect"
                  >
                </label>
                <button
                  v-if="user?.avatar"
                  type="button"
                  class="profile-avatar-action profile-avatar-delete"
                  :disabled="loadingAvatar"
                  :title="t('delete')"
                  @click="handleAvatarDelete"
                >
                  <i class="profile-avatar-x">×</i>
                </button>
              </div>
            </div>

            <h4 class="mb-1 fw-bold">
              {{ profileForm.fullName || displayName }}
            </h4>
            <p class="text-muted small mb-4">
              {{ profileForm.phone || t('notSpecified') }}
            </p>

            <form
              class="text-start"
              @submit.prevent="handleProfileSubmit"
            >
              <div class="mb-3">
                <label class="form-label fw-medium text-muted small">{{ t('fullName') || 'Full Name' }}</label>
                <input
                  v-model="profileForm.fullName"
                  type="text"
                  class="form-control"
                  required
                >
              </div>
              <div class="mb-4">
                <label class="form-label fw-medium text-muted small">{{ t('phone') || 'Phone' }}</label>
                <input
                  v-model="profileForm.phone"
                  type="text"
                  class="form-control"
                >
              </div>

              <button
                type="submit"
                class="btn btn-primary w-100"
                :disabled="loadingProfile"
              >
                <span
                  v-if="loadingProfile"
                  class="spinner-border spinner-border-sm me-2"
                />
                <i class="bi bi-person-check me-2" />{{ t('save') }}
              </button>
            </form>
          </div>
        </div>
      </div>

      <!-- Right Column: Security & Actions -->
      <div class="col-lg-8">
        <div class="card mb-4 border-0 shadow-sm">
          <div class="card-header bg-transparent border-0 pt-4 pb-0">
            <h5 class="fw-bold mb-0">
              <i class="bi bi-shield-lock me-2 text-primary" />{{ t('changePassword') }}
            </h5>
          </div>
          <div class="card-body pt-4">
            <form @submit.prevent="handlePasswordSubmit">
              <div class="row">
                <div class="col-md-12 mb-3">
                  <label class="form-label fw-medium text-muted small">{{ t('currentPassword') }}</label>
                  <input
                    v-model="passwordForm.oldPassword"
                    type="password"
                    class="form-control"
                    required
                  >
                </div>
                <div class="col-md-6 mb-3">
                  <label class="form-label fw-medium text-muted small">{{ t('newPassword') }}</label>
                  <input
                    v-model="passwordForm.newPassword"
                    type="password"
                    class="form-control"
                    required
                    minlength="8"
                  >
                </div>
                <div class="col-md-6 mb-4">
                  <label class="form-label fw-medium text-muted small">{{ t('confirmPassword') }}</label>
                  <input
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    class="form-control"
                    required
                    minlength="8"
                  >
                </div>
              </div>

              <div class="d-flex justify-content-end">
                <button
                  type="submit"
                  class="btn btn-primary px-4"
                  :disabled="loadingPassword"
                >
                  <span
                    v-if="loadingPassword"
                    class="spinner-border spinner-border-sm me-2"
                  />
                  <i class="bi bi-key me-2" />{{ t('changePassword') }}
                </button>
              </div>
            </form>
          </div>
        </div>

        <div class="card border-0 shadow-sm border-danger border-start border-4">
          <div class="card-body d-flex align-items-center justify-content-between p-4">
            <div>
              <h5 class="fw-bold mb-1">
                {{ t('logoutAllDevices') || 'Log out all devices' }}
              </h5>
            </div>
            <button
              type="button"
              class="btn btn-outline-danger"
              @click="logoutAll"
            >
              <i class="bi bi-box-arrow-right me-2" /> {{ t('logoutAllDevices') || 'Log out all devices' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </AppContent>
</template>
