import { useState } from 'react'
import { PANEL_MIN_HEIGHT, PANEL_MIN_WIDTH, SCREEN_MARGIN } from './geometry'
import type { Coordinates, PanelSize } from './types'

export type ResizeDirection = 'n' | 's' | 'e' | 'w' | 'nw' | 'ne' | 'sw' | 'se'

export const RESIZE_HANDLES: ResizeDirection[] = ['nw', 'n', 'ne', 'w', 'e', 'sw', 's', 'se']

/**
 * 패널 가장자리 핸들을 끌어 크기를 바꾼다. 아래·오른쪽으로 늘리면 패널이 화면 오른쪽 아래에 고정돼 있으므로 좌표도 함께 옮긴다.
 */
export function usePanelResize({ panelRef, coordinates, setPanelSize, onUpdateCoordinates }: {
  panelRef: React.RefObject<HTMLElement | null>
  coordinates: Coordinates
  setPanelSize: (size: PanelSize) => void
  onUpdateCoordinates: (coords: Coordinates) => void
}) {
  const [isResizing, setIsResizing] = useState(false)

  const handleResizeStart = (
    direction: ResizeDirection,
    event: React.PointerEvent<HTMLDivElement>
  ) => {
    if (event.button !== 0) return
    if (!panelRef.current) return
    event.preventDefault()
    event.stopPropagation()

    const startRect = panelRef.current.getBoundingClientRect()
    const startPointerX = event.clientX
    const startPointerY = event.clientY
    const startCoordX = coordinates.x
    const startCoordY = coordinates.y

    setIsResizing(true)

    const handlePointerMove = (e: PointerEvent) => {
      e.preventDefault()
      const dx = e.clientX - startPointerX
      const dy = e.clientY - startPointerY

      let newWidth = startRect.width
      let newHeight = startRect.height
      let nextX = startCoordX
      let nextY = startCoordY

      // Vertical resizing
      if (direction.includes('n')) {
        const targetTop = startRect.top + dy
        const maxTop = startRect.bottom - PANEL_MIN_HEIGHT
        const clampedTop = Math.min(maxTop, Math.max(SCREEN_MARGIN, targetTop))
        newHeight = Math.round(startRect.bottom - clampedTop)
      } else if (direction.includes('s')) {
        const targetBottom = startRect.bottom + dy
        const minBottom = startRect.top + PANEL_MIN_HEIGHT
        const clampedBottom = Math.max(minBottom, Math.min(window.innerHeight - SCREEN_MARGIN, targetBottom))
        newHeight = Math.round(clampedBottom - startRect.top)
        nextY = Math.round(clampedBottom - (window.innerHeight - 88))
      }

      // Horizontal resizing
      if (direction.includes('w')) {
        const targetLeft = startRect.left + dx
        const maxLeft = startRect.right - PANEL_MIN_WIDTH
        const clampedLeft = Math.min(maxLeft, Math.max(SCREEN_MARGIN, targetLeft))
        newWidth = Math.round(startRect.right - clampedLeft)
      } else if (direction.includes('e')) {
        const targetRight = startRect.right + dx
        const minRight = startRect.left + PANEL_MIN_WIDTH
        const clampedRight = Math.max(minRight, Math.min(window.innerWidth - SCREEN_MARGIN, targetRight))
        newWidth = Math.round(clampedRight - startRect.left)
        nextX = Math.round(clampedRight - (window.innerWidth - 24))
      }

      setPanelSize({ width: newWidth, height: newHeight })
      if (nextX !== startCoordX || nextY !== startCoordY) {
        onUpdateCoordinates({ x: nextX, y: nextY })
      }
    }

    const handlePointerUp = () => {
      setIsResizing(false)
      window.removeEventListener('pointermove', handlePointerMove)
      window.removeEventListener('pointerup', handlePointerUp)
      window.removeEventListener('pointercancel', handlePointerUp)
    }

    window.addEventListener('pointermove', handlePointerMove)
    window.addEventListener('pointerup', handlePointerUp)
    window.addEventListener('pointercancel', handlePointerUp)
  }

  return { isResizing, handleResizeStart }
}
