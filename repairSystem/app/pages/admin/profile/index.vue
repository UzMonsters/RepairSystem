<script setup lang="ts">
import { getApiErrorMessage } from '~/utils/api'
import type { LanguageCode, UserDateFormat, UserTimeFormat, UserTheme } from '~/types'

const { user, updateProfile, uploadAvatar, deleteAvatar, changePassword, logout } = useAuth()
const { t } = useLocale()

const loadingProfile = ref(false)
const loadingAvatar = ref(false)
const loadingPassword = ref(false)

const profileForm = ref({
  fullName: user.value?.fullName || '',
  phone: user.value?.phone || '',
  language: user.value?.language || 'UZ' as LanguageCode,
  dateFormat: user.value?.dateFormat || 'DD_SLASH_MM_SLASH_YYYY' as UserDateFormat,
  timeFormat: user.value?.timeFormat || 'HOUR_24' as UserTimeFormat,
  theme: user.value?.theme || 'SYSTEM' as UserTheme
})

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const avatarFile = ref<File | null>(null)
const avatarPreviewUrl = ref<string | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

function handleFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    avatarFile.value = target.files[0]
    avatarPreviewUrl.value = URL.createObjectURL(avatarFile.value)
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

async function handleAvatarSubmit() {
  if (!avatarFile.value) return
  loadingAvatar.value = true
  try {
    await uploadAvatar(avatarFile.value)
    avatarFile.value = null
    avatarPreviewUrl.value = null
    if (fileInput.value) fileInput.value.value = ''
    useToast().showSuccess(t('savedSuccessfully'))
  } catch (e) {
    useToast().showError(getApiErrorMessage(e, 'Failed to upload avatar'))
  } finally {
    loadingAvatar.value = false
  }
}

async function handleAvatarDelete() {
  if (!confirm(t('confirmDelete') || 'Are you sure?')) return
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
      <div class="col-lg-6">
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

              <hr class="my-4">
              <h5 class="mb-3">
                {{ t('personalSettings') || 'Preferences' }}
              </h5>

              <div class="mb-3">
                <label class="form-label">{{ t('language') }}</label>
                <select
                  v-model="profileForm.language"
                  class="form-select"
                  required
                >
                  <option value="UZ">
                    {{ t('uzbek') }}
                  </option>
                  <option value="RU">
                    {{ t('russian') }}
                  </option>
                  <option value="EN">
                    English
                  </option>
                </select>
              </div>
              <div class="mb-3">
                <label class="form-label">{{ t('dateFormat') }}</label>
                <select
                  v-model="profileForm.dateFormat"
                  class="form-select"
                  required
                >
                  <option value="DD_SLASH_MM_SLASH_YYYY">
                    DD/MM/YYYY
                  </option>
                  <option value="DD_MM_YYYY">
                    DD.MM.YYYY
                  </option>
                  <option value="YYYY_MM_DD">
                    YYYY-MM-DD
                  </option>
                </select>
              </div>
              <div class="mb-3">
                <label class="form-label">{{ t('timeFormat') }}</label>
                <select
                  v-model="profileForm.timeFormat"
                  class="form-select"
                  required
                >
                  <option value="HOUR_24">
                    24 {{ t('hourFormat') || 'Hour' }}
                  </option>
                  <option value="HOUR_12">
                    12 {{ t('hourFormat') || 'Hour' }}
                  </option>
                </select>
              </div>
              <div class="mb-4">
                <label class="form-label">{{ t('theme') }}</label>
                <select
                  v-model="profileForm.theme"
                  class="form-select"
                  required
                >
                  <option value="SYSTEM">
                    {{ t('system') || 'System' }}
                  </option>
                  <option value="LIGHT">
                    {{ t('light') || 'Light' }}
                  </option>
                  <option value="DARK">
                    {{ t('dark') || 'Dark' }}
                  </option>
                </select>
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

      <div class="col-lg-6 d-flex flex-column gap-4">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">
              {{ t('avatar') || 'Avatar' }}
            </h3>
          </div>
          <div class="card-body">
            <div class="d-flex align-items-center gap-4 mb-4">
              <img
                v-if="avatarPreviewUrl || user?.avatar?.url"
                :src="avatarPreviewUrl || user?.avatar?.url"
                alt="Avatar"
                class="rounded-circle shadow object-fit-cover"
                style="width: 80px; height: 80px;"
              >
              <div
                v-else
                class="d-flex align-items-center justify-content-center rounded-circle bg-secondary text-white shadow"
                style="width: 80px; height: 80px; font-size: 24px; font-weight: bold;"
              >
                {{ (user?.fullName || 'A').charAt(0) }}
              </div>

              <div class="flex-grow-1">
                <input
                  ref="fileInput"
                  type="file"
                  class="form-control mb-2"
                  accept="image/jpeg,image/png,image/webp"
                  @change="handleFileSelect"
                >
                <div class="d-flex gap-2">
                  <button
                    class="btn btn-primary"
                    :disabled="!avatarFile || loadingAvatar"
                    @click="handleAvatarSubmit"
                  >
                    <span
                      v-if="loadingAvatar"
                      class="spinner-border spinner-border-sm me-2"
                    />
                    {{ t('upload') || 'Upload' }}
                  </button>
                  <button
                    v-if="user?.avatar"
                    class="btn btn-outline-danger"
                    :disabled="loadingAvatar"
                    @click="handleAvatarDelete"
                  >
                    {{ t('delete') }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="card flex-grow-1">
          <div class="card-header">
            <h3 class="card-title">
              {{ t('changePassword') || 'Change Password' }}
            </h3>
          </div>
          <div class="card-body">
            <form @submit.prevent="handlePasswordSubmit">
              <div class="mb-3">
                <label class="form-label">{{ t('currentPassword') || 'Current Password' }}</label>
                <input
                  v-model="passwordForm.oldPassword"
                  type="password"
                  class="form-control"
                  required
                >
              </div>
              <div class="mb-3">
                <label class="form-label">{{ t('newPassword') || 'New Password' }}</label>
                <input
                  v-model="passwordForm.newPassword"
                  type="password"
                  class="form-control"
                  required
                  minlength="8"
                >
              </div>
              <div class="mb-4">
                <label class="form-label">{{ t('confirmPassword') || 'Confirm Password' }}</label>
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
                class="btn btn-warning w-100"
                :disabled="loadingPassword"
              >
                <span
                  v-if="loadingPassword"
                  class="spinner-border spinner-border-sm me-2"
                />
                {{ t('changePassword') || 'Change Password' }}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  </AppContent>
</template>
