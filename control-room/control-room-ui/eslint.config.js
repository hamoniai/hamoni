// @ts-check
const eslint = require('@eslint/js');
const { defineConfig } = require('eslint/config');
const eslintConfigPrettier = require("eslint-config-prettier/flat");
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');
const licenseHeader = require("eslint-plugin-license-header");

module.exports = defineConfig([
  {
    files: ['**/*.ts'],
    plugins: {
      "license-header": licenseHeader,
    },
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      'no-console': 'error',
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          prefix: 'app',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          prefix: 'app',
          style: 'kebab-case',
        },
      ],
      "@stylistic/indent": ['error', 2],
      '@stylistic/quotes': ['error', 'single', { avoidEscape: true }],
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    rules: {},
  },
  eslintConfigPrettier,
]);
