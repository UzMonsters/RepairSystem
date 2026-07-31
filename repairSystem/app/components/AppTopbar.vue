<script setup>
const user = {
  name: 'Alexander Pierce',
  image: '/assets/img/user2-160x160.jpg',
  role: 'Web Developer',
  memberSince: 'Nov. 2023'
}

const messages = [
  { from: 'Brad Diesel', text: 'Call me whenever you can...', time: '4 Hours Ago', star: 'danger', image: '/assets/img/user1-128x128.jpg' },
  { from: 'John Pierce', text: 'I got your message bro', time: '4 Hours Ago', star: 'secondary', image: '/assets/img/user8-128x128.jpg' },
  { from: 'Nora Silvester', text: 'The subject goes here', time: '4 Hours Ago', star: 'warning', image: '/assets/img/user3-128x128.jpg' }
]

const notifications = [
  { text: '4 new messages', icon: 'bi-envelope', time: '3 mins' },
  { text: '8 friend requests', icon: 'bi-people-fill', time: '12 hours' },
  { text: '3 new reports', icon: 'bi-file-earmark-fill', time: '2 days' }
]
</script>

<template>
  <nav class="app-header navbar navbar-expand bg-body">
    <div class="container-fluid">
      <ul class="navbar-nav">
        <li class="nav-item">
          <button
            type="button"
            class="nav-link"
            title="Toggle sidebar"
            aria-label="Toggle sidebar"
            data-lte-toggle="sidebar"
          >
            <i class="bi bi-list" />
          </button>
        </li>
      </ul>

      <ul class="navbar-nav ms-auto">
        <NavMessages :messages="messages" />
        <NavNotifications :notifications="notifications" />
        <FullscreenToggle />
        <ColorModeToggle />

        <li class="nav-item dropdown user-menu">
          <a
            href="#"
            class="nav-link dropdown-toggle"
            data-bs-toggle="dropdown"
            @click.prevent
          >
            <img
              :src="user.image"
              class="user-image rounded-circle shadow"
              :alt="user.name"
            >
            <span class="d-none d-md-inline">{{ user.name }}</span>
          </a>
          <ul class="dropdown-menu dropdown-menu-lg dropdown-menu-end">
            <li class="user-header text-bg-primary">
              <img
                :src="user.image"
                class="rounded-circle shadow"
                :alt="user.name"
              >
              <p>
                {{ user.name }} - {{ user.role }}
                <small>Member since {{ user.memberSince }}</small>
              </p>
            </li>
            <li class="user-body">
              <div class="row">
                <div class="col-4 text-center">
                  <a href="#">Followers</a>
                </div>
                <div class="col-4 text-center">
                  <a href="#">Sales</a>
                </div>
                <div class="col-4 text-center">
                  <a href="#">Friends</a>
                </div>
              </div>
            </li>
            <li class="user-footer">
              <a
                href="#"
                class="btn btn-outline-secondary"
              >Profile</a>
              <a
                href="#"
                class="btn btn-outline-danger float-end"
              >Sign out</a>
            </li>
          </ul>
        </li>
      </ul>
    </div>
  </nav>
</template>
