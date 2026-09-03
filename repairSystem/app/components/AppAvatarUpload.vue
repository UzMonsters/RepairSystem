<script setup lang="ts">
import { getApiErrorMessage } from '~/utils/api'

const props = defineProps<{
  uploadUrl: string
  deleteUrl?: string
  initialAvatarUrl?: string | null
  hasAvatarInit?: boolean
  initials?: string
}>()

const emit = defineEmits<{
  (e: 'updated', url: string | null): void
  (e: 'deleted'): void
}>()

const { t } = useLocale()
const loading = ref(false)
const previewUrl = ref<string | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const ts = ref(Date.now())

const avatarSrc = computed(() => {
  if (previewUrl.value) return previewUrl.value
  if (props.initialAvatarUrl) return `${props.initialAvatarUrl}?t=${ts.value}`
  return undefined
})

const hasAvatar = computed(() => !!previewUrl.value || props.hasAvatarInit)

async function handleFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  previewUrl.value = URL.createObjectURL(file)
  loading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    await apiFetch(props.uploadUrl, {
      method: 'PUT',
      body: formData
    })
    previewUrl.value = null
    ts.value = Date.now()
    emit('updated', props.initialAvatarUrl)
    if (fileInput.value) fileInput.value.value = ''
    useToast().showSuccess(t('savedSuccessfully'))
  } catch (error) {
    previewUrl.value = null
    useToast().showError(getApiErrorMessage(error, 'Failed to upload avatar'))
  } finally {
    loading.value = false
  }
}

async function handleAvatarDelete() {
  if (!props.deleteUrl) return
  loading.value = true
  try {
    await apiFetch(props.deleteUrl, { method: 'DELETE' })
    emit('deleted')
    ts.value = Date.now()
    useToast().showSuccess(t('deletedSuccessfully'))
  } catch (e) {
    useToast().showError(getApiErrorMessage(e, 'Failed to delete avatar'))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="profile-avatar-editor mx-auto mb-3" style="width: fit-content;">
    <div
      class="profile-avatar-frame mx-auto"
      :class="{ 'has-avatar': hasAvatar }"
    >
      <img
        v-if="hasAvatar && avatarSrc"
        :src="avatarSrc"
        alt="Avatar"
        class="rounded-circle shadow object-fit-cover w-100 h-100"
      >
      <div
        v-else
        class="d-flex align-items-center justify-content-center rounded-circle bg-primary text-white shadow h-100 w-100"
      >
        <span class="fw-bold fs-2">{{ initials || 'U' }}</span>
      </div>
      <label
        class="profile-avatar-action profile-avatar-upload"
        :class="{ disabled: loading }"
        :title="t('chooseFile')"
      >
        <i v-if="!loading" class="bi bi-camera-fill profile-avatar-camera" />
        <span v-else class="spinner-border spinner-border-sm" />
        <input
          ref="fileInput"
          type="file"
          accept="image/jpeg,image/png,image/webp"
          :disabled="loading"
          @change="handleFileSelect"
        >
      </label>
      <button
        v-if="hasAvatar && deleteUrl"
        type="button"
        class="profile-avatar-action profile-avatar-delete"
        :disabled="loading"
        :title="t('delete')"
        @click="handleAvatarDelete"
      >
        <i class="profile-avatar-x">×</i>
      </button>
    </div>
  </div>
</template>
