;(function () {
  'use strict'

  function prepareSvgbob() {
    var diagrams = document.querySelectorAll('svg.svgbob')
    for (var index = 0; index < diagrams.length; index++) {
      var diagram = diagrams[index]
      if (diagram.getAttribute('viewBox')) continue
      var width = diagram.getAttribute('width')
      var height = diagram.getAttribute('height')
      if (width && height) {
        diagram.setAttribute('viewBox', '0 0 ' + parseFloat(width) + ' ' + parseFloat(height))
      }
    }
  }

  function wrapTables() {
    var tables = document.querySelectorAll('.doc table.tableblock')
    for (var index = 0; index < tables.length; index++) {
      var table = tables[index]
      if (table.parentNode && table.parentNode.classList.contains('table-scroll')) continue
      var wrapper = document.createElement('div')
      wrapper.className = 'table-scroll'
      table.parentNode.insertBefore(wrapper, table)
      wrapper.appendChild(table)
    }
  }

  function enhance() {
    prepareSvgbob()
    wrapTables()
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', enhance)
  } else {
    enhance()
  }
})()
