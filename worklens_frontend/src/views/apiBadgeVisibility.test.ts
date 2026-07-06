import { readdirSync, readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('view API badge visibility', () => {
  it('does not expose raw API paths in view templates', () => {
    const viewsDir = join(process.cwd(), 'src', 'views')
    const offenders = readdirSync(viewsDir)
      .filter((fileName) => fileName.endsWith('.vue'))
      .flatMap((fileName) => {
        const source = readFileSync(join(viewsDir, fileName), 'utf8')
        const hasPanelBadge = source.includes('panel-badge')
        const hasRawApiPath = /\b(?:GET|POST|PUT|DELETE)\s+\/[A-Za-z0-9{}_$`/.-]+/.test(source)
        const hasBacktickedApiPath =
          /`\/(?:employees|usage-records|team-usage-summary|detail-access-requests|llm\/)[^`]*`/.test(source)

        return hasPanelBadge || hasRawApiPath || hasBacktickedApiPath ? [fileName] : []
      })

    expect(offenders).toEqual([])
  })
})
