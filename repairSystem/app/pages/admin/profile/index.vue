<script setup lang="ts">
import { getApiErrorMessage } from '~/utils/api'

const { user, updateProfile, uploadAvatar, deleteAvatar, changePassword, logout, avatarObjectUrl } = useAuth()
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
    <div class="row g-4 profile-page-grid">
      <div class="col-lg-6 profile-main-column">
        <div class="card h-100">
          <div class="card-header">
            <h3 class="card-title">
              {{ t('profile') || 'Profile Details' }}
            </h3>
          </div>
          <div class="card-body">
            <form @submit.prevent="handleProfileSubmit">
              <div class="mb-3">
                <label class="form-label">{{ t('fullName') || 'Full Name' }}</label>
                <input
                  v-model="profileForm.fullName"
                  type="text"
                  class="form-control"
                  required
                >
              </div>
              <div class="mb-3">
                <label class="form-label">{{ t('phone') || 'Phone' }}</label>
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
                {{ t('save') }}
              </button>
            </form>
          </div>
        </div>
      </div>

      <div class="col-lg-6 d-flex flex-column gap-4 profile-side-column">
        <div class="card profile-avatar-card">
          <div class="card-header">
            <h3 class="card-title">
              <i class="bi bi-person-circle me-2" />{{ t('avatar') }}
            </h3>
          </div>
          <div class="card-body">
            <div class="profile-avatar-editor">
              <div
                class="profile-avatar-frame"
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
                  class="d-flex align-items-center justify-content-center rounded-circle bg-secondary text-white shadow"
                />
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
              <p class="text-muted small mb-0">
                {{ t('avatarHint') }}
              </p>
            </div>
          </div>
        </div>

        <div class="card flex-grow-1 profile-password-card">
          <div class="card-header">
            <h3 class="card-title">
              <i class="bi bi-shield-lock me-2" />{{ t('changePassword') }}
            </h3>
          </div>
          <div class="card-body">
            <form @submit.prevent="handlePasswordSubmit">
              <div class="mb-3">
                <label class="form-label">{{ t('currentPassword') }}</label>
                <input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  class="form-control"
                  required
                >
              </div>
              <div class="mb-3">
                <label class="form-label">{{ t('newPassword') }}</label>
                <input
                  v-model="passwordForm.newPassword"
                  type="password"
                  class="form-control"
                  required
                  minlength="8"
                >
              </div>
              <div class="mb-4">
                <label class="form-label">{{ t('confirmPassword') }}</label>
                <input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  class="form-control"
                  required
                  minlength="8"
                >
              </div>

              <button
                type="submit"
                class="btn btn-primary w-100"
                :disabled="loadingPassword"
              >
                <span
                  v-if="loadingPassword"
                  class="spinner-border spinner-border-sm me-2"
                />
                <i class="bi bi-key me-2" />{{ t('changePassword') }}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  </AppContent>
</template>
