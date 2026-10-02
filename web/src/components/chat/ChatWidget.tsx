import { useEffect, useRef, useState } from 'react'
import {
  DndContext,
  useSensor,
  useSensors,
  PointerSensor,
  type DragEndEvent,
  type DragMoveEvent,
} from '@dnd-kit/core'
import { useNavigate } from 'react-router-dom'
import { ChatLauncher } from './ChatLauncher'
import { ChatPanel } from './ChatPanel'
import { clampCoordinates, fitPanelSize } from './geometry'
import type { BookWithSymptom, Coordinates, PanelSize } from './types'
import './ChatWidget.css'

interface ChatWidgetProps {
  isOpen: boolean
  onToggle: () => void
  onBookWithSymptom: BookWithSymptom
}

/**
 * 화면 구석의 AI 상담 런처와 패널. 둘은 같은 좌표를 공유하며 끌어서 옮길 수 있다.
 */
export function ChatWidget({ isOpen, onToggle, onBookWithSymptom }: ChatWidgetProps) {
  const navigate = useNavigate()
  const [coordinates, setCoordinates] = useState<Coordinates>({ x: 0, y: 0 })
  const [panelSize, setPanelSize] = useState<PanelSize | null>(null)
  const [dragDelta, setDragDelta] = useState<Coordinates | null>(null)
  const launcherRef = useRef<HTMLButtonElement | null>(null)
  const panelRef = useRef<HTMLElement | null>(null)
  const isDraggingRef = useRef(false)

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 6,
      },
    })
  )

  const activeTarget = () => (isOpen && panelRef.current) ? panelRef.current : launcherRef.current

  useEffect(() => {
    const handleResize = () => {
      setCoordinates(prev => {
        const target = (isOpen && panelRef.current) ? panelRef.current : launcherRef.current
        return clampCoordinates(target, prev.x, prev.y)
      })
      setPanelSize(fitPanelSize)
    }

    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [isOpen])

  useEffect(() => {
    if (isOpen) {
      const raf = requestAnimationFrame(() => {
        if (panelRef.current) {
          setCoordinates(prev => clampCoordinates(panelRef.current, prev.x, prev.y))
        }
      })
      return () => cancelAnimationFrame(raf)
    }
  }, [isOpen])

  const releaseDragging = () => {
    // 드래그를 놓는 순간의 클릭이 런처 토글로 처리되지 않게 잠시 뒤에 해제한다.
    setTimeout(() => {
      isDraggingRef.current = false
    }, 100)
  }

  const handleDragStart = () => {
    isDraggingRef.current = true
    setDragDelta({ x: 0, y: 0 })
  }

  const handleDragMove = (event: DragMoveEvent) => {
    const next = clampCoordinates(activeTarget(), coordinates.x + event.delta.x, coordinates.y + event.delta.y)
    setDragDelta({ x: next.x - coordinates.x, y: next.y - coordinates.y })
  }

  const handleDragEnd = (event: DragEndEvent) => {
    setDragDelta(null)
    if (event.delta) {
      setCoordinates(prev => clampCoordinates(activeTarget(), prev.x + event.delta.x, prev.y + event.delta.y))
    }
    releaseDragging()
  }

  const handleDragCancel = () => {
    setDragDelta(null)
    releaseDragging()
  }

  const current: Coordinates = {
    x: coordinates.x + (dragDelta ? dragDelta.x : 0),
    y: coordinates.y + (dragDelta ? dragDelta.y : 0),
  }

  return (
    <DndContext
      sensors={sensors}
      autoScroll={false}
      onDragStart={handleDragStart}
      onDragMove={handleDragMove}
      onDragEnd={handleDragEnd}
      onDragCancel={handleDragCancel}
    >
      <ChatLauncher
        isOpen={isOpen}
        onToggle={() => {
          if (!isDraggingRef.current) {
            onToggle()
          }
        }}
        coordinates={current}
        launcherRef={launcherRef}
      />
      <ChatPanel
        isOpen={isOpen}
        onClose={onToggle}
        onBookWithSymptom={onBookWithSymptom}
        onNavigate={page => { navigate(`/${page}`); onToggle() }}
        coordinates={current}
        panelRef={panelRef}
        panelSize={panelSize}
        setPanelSize={setPanelSize}
        onUpdateCoordinates={setCoordinates}
      />
    </DndContext>
  )
}
