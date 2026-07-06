import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('restrained internal tool visual style', () => {
  it('hides text carets in read-only display areas while preserving form input carets', () => {
    const globalStyle = readFileSync(join(process.cwd(), 'src', 'style.css'), 'utf8')

    expect(globalStyle).toMatch(/body\s*{[\s\S]*caret-color:\s*transparent;/)
    expect(globalStyle).toMatch(/input,\s*\ntextarea\s*{[\s\S]*caret-color:\s*auto;/)
    expect(globalStyle).toMatch(/input,\s*\ntextarea\s*{[\s\S]*cursor:\s*text;/)
  })

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

  it('lets generated report content wrap and grow with long text', () => {
    const viewsDir = join(process.cwd(), 'src', 'views')

    for (const fileName of ['EmployeeHomeView.vue', 'ManagerTeamView.vue']) {
      const source = readFileSync(join(viewsDir, fileName), 'utf8')

      expect(source).toMatch(/\.report-card\s*{[\s\S]*height:\s*auto;/)
      expect(source).toMatch(/\.report-card\s*{[\s\S]*overflow:\s*visible;/)
      expect(source).toMatch(/\.report-card p[\s\S]*{[\s\S]*white-space:\s*pre-wrap;/)
      expect(source).toMatch(/\.report-card p[\s\S]*{[\s\S]*overflow-wrap:\s*anywhere;/)
    }
  })
})
