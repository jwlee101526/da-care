import type { Coordinates, PanelSize } from './types'

export const PANEL_MIN_WIDTH = 340
export const PANEL_MIN_HEIGHT = 430
export const SCREEN_MARGIN = 12

/** 런처나 패널이 화면 밖으로 나가지 않도록 이동 좌표를 제한한다. */
export function clampCoordinates(
  targetElement: HTMLElement | null,
  targetX: number,
  targetY: number
): Coordinates {
  if (!targetElement) {
    return { x: targetX, y: targetY }
  }

  const style = window.getComputedStyle(targetElement)
  const right = parseFloat(style.right) || 24
  const bottom = parseFloat(style.bottom) || 24
  const width = targetElement.offsetWidth || 390
  const height = targetElement.offsetHeight || 580

  const minX = SCREEN_MARGIN + right + width - window.innerWidth
  const maxX = Math.max(0, right - SCREEN_MARGIN)
  const minY = SCREEN_MARGIN + bottom + height - window.innerHeight
  const maxY = Math.max(0, bottom - SCREEN_MARGIN)

  const safeMinX = Math.min(minX, maxX)
  const safeMaxX = Math.max(minX, maxX)
  const safeMinY = Math.min(minY, maxY)
  const safeMaxY = Math.max(minY, maxY)

  return {
    x: Math.min(Math.max(targetX, safeMinX), safeMaxX),
    y: Math.min(Math.max(targetY, safeMinY), safeMaxY),
  }
}

/** 창이 줄어들면 사용자가 늘려 둔 패널 크기를 화면 안으로 줄인다. */
export function fitPanelSize(size: PanelSize | null): PanelSize | null {
  if (!size) return null
  const maxW = Math.max(PANEL_MIN_WIDTH, window.innerWidth - 24)
  const maxH = Math.max(PANEL_MIN_HEIGHT, window.innerHeight - 24)
  if (size.width > maxW || size.height > maxH) {
    return {
      width: Math.min(size.width, maxW),
      height: Math.min(size.height, maxH),
    }
  }
  return size
}
