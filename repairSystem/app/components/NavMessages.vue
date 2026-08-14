<script setup>
defineProps({
  messages: { type: Array, default: () => [] },
  seeAllUrl: { type: String, default: '#' },
  seeAllText: { type: String, default: 'See All Messages' }
})
</script>

<template>
  <li class="nav-item dropdown">
    <a
      class="nav-link"
      data-bs-toggle="dropdown"
      href="#"
      @click.prevent
    >
      <i class="bi bi-chat-text" />
      <span
        v-if="messages.length"
        class="navbar-badge badge text-bg-danger"
      >{{ messages.length }}</span>
    </a>
    <div class="dropdown-menu dropdown-menu-lg dropdown-menu-end">
      <template
        v-for="(msg, idx) in messages"
        :key="idx"
      >
        <a
          :href="msg.url || '#'"
          class="dropdown-item"
        >
          <div class="d-flex">
            <div
              v-if="msg.image"
              class="flex-shrink-0"
            >
              <img
                :src="msg.image"
                alt="User Avatar"
                class="img-size-50 rounded-circle me-3"
              >
            </div>
            <div class="flex-grow-1">
              <h3 class="dropdown-item-title">
                {{ msg.from }}
                <span
                  v-if="msg.star"
                  :class="`float-end fs-7 text-${msg.star}`"
                ><i class="bi bi-star-fill" /></span>
              </h3>
              <p class="fs-7">{{ msg.text }}</p>
              <p
                v-if="msg.time"
                class="fs-7 text-secondary"
              ><i class="bi bi-clock-fill me-1" /> {{ msg.time }}</p>
            </div>
          </div>
        </a>
        <div class="dropdown-divider" />
      </template>
      <a
        :href="seeAllUrl"
        class="dropdown-item dropdown-footer"
      >{{ seeAllText }}</a>
    </div>
  </li>
</template>
