module.exports = {
  extends: ['@commitlint/config-conventional'],
  parserPreset: { parserOpts: { issuePrefixes: ['MED-'] } },
  rules: {
    'scope-empty': [2, 'never'],
    'scope-enum': [2, 'always', [
      'ms01','ms02','ms03','ms04','ms05','ms06','ms07','ms08','ms09','ms10','ms11','ms12',
      'web','mobile','desktop','n8n','tools','contracts','infra','deploy','ci','docs','repo'
    ]],
    'header-max-length': [2, 'always', 72],
    'references-empty': [2, 'never']
  }
};
