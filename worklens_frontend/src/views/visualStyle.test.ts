import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('restrained internal tool visual style', () => {
  it('does not use decorative shell backgrounds', () => {
    const appSource = readFileSync(join(process.cwd(), 'src', 'App.vue'), 'utf8')

    expect(appSource).not.toContain('app-shell__noise')
    expect(appSource).not.toContain('app-shell__orb')
    expect(appSource).not.toContain('radial-gradient')
    expect(appSource).not.toContain('linear-gradient')
  })

  it('keeps page headers simple and non-promotional', () => {
    const viewsDir = join(process.cwd(), 'src', 'views')
    const offenders = readdirSync(viewsDir)
      .filter((fileName) => fileName.endsWith('View.vue'))
      .flatMap((fileName) => {
        const source = readFileSync(join(viewsDir, fileName), 'utf8')
        const hasDecorativeHeader = /hero-card[\s\S]*?(?:radial-gradient|linear-gradient|font-family:\s*Georgia)/.test(
          source,
        )

        return hasDecorativeHeader ? [fileName] : []
      })

    expect(offenders).toEqual([])
  })
})
