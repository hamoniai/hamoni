;(function () {
  'use strict'

  var root = document.documentElement
  var button = document.querySelector('[data-theme-toggle]')
  if (!button) return

  var media = window.matchMedia('(prefers-color-scheme: dark)')

  function storedTheme() {
    try {
      return localStorage.getItem('hamoni-theme')
    } catch (error) {
      return null
    }
  }

  function isDark() {
    return root.classList.contains('app-dark')
  }

  function updateButton() {
    var dark = isDark()
    var label = dark ? 'Switch to light theme' : 'Switch to dark theme'
    button.setAttribute('aria-label', label)
    button.setAttribute('title', label)
    button.setAttribute('aria-pressed', String(dark))
  }

  function setTheme(theme) {
    root.classList.toggle('app-dark', theme === 'dark')
    try {
      localStorage.setItem('hamoni-theme', theme)
    } catch (error) {
      // Local storage may be unavailable; the theme still applies for this page.
    }
    updateButton()
  }

  button.addEventListener('click', function () {
    setTheme(isDark() ? 'light' : 'dark')
  })

  function onMediaChange(event) {
    if (!storedTheme()) {
      root.classList.toggle('app-dark', event.matches)
      updateButton()
    }
  }

  if (media.addEventListener) {
    media.addEventListener('change', onMediaChange)
  } else if (media.addListener) {
    media.addListener(onMediaChange)
  }

  updateButton()
})()
