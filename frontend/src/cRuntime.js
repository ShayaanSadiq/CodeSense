import JSCPP from 'JSCPP'

export function prepareC(source) {
  return String(source || '')
    .replace(/#include\s*<stdio\.h>/g, '#include <iostream>\nusing namespace std;')
    .replace(/#include\s*<cstdio>/g, '#include <iostream>\nusing namespace std;')
    .replace(/#include\s*<string\.h>/g, '')
    .replace(/#include\s*<cstring>/g, '')
    .replace(/\bint\s+main\s*\(\s*void\s*\)/g, 'int main()')
    .replace(/printf\s*\(\s*"%d\\n"\s*,\s*([^;]+?)\)\s*;/g, 'cout << $1 << endl;')
    .replace(/printf\s*\(\s*"%s\\n"\s*,\s*([^;]+?)\)\s*;/g, 'cout << $1 << endl;')
    .replace(/printf\s*\(\s*"%d"\s*,\s*([^;]+?)\)\s*;/g, 'cout << $1;')
}

export function runC(source, stdin = '', onWrite) {
  const code = prepareC(source)
  if (!code.trim()) {
    throw new Error('There is no C to run.')
  }
  let output = ''
  const exitCode = JSCPP.run(code, stdin || '', {
    maxTimeout: 4000,
    unsigned_overflow: 'ignore',
    stdio: {
      write(text) {
        output += text
        if (onWrite) onWrite(text)
      },
    },
  })
  return { stdout: output, exitCode }
}
