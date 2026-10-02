import { useDraggable } from '@dnd-kit/core'
import { MessageCircle } from 'lucide-react'
import { useLanguage } from '../../context/LanguageContext'
import type { Coordinates } from './types'

interface ChatLauncherProps {
  isOpen: boolean
  onToggle: () => void
  coordinates: Coordinates
  launcherRef: React.RefObject<HTMLButtonElement | null>
}

export function ChatLauncher({ isOpen, onToggle, coordinates, launcherRef }: ChatLauncherProps) {
  const { t } = useLanguage()
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: 'chat-launcher',
  })

  const handleRef = (node: HTMLButtonElement | null) => {
    setNodeRef(node)
    launcherRef.current = node
  }

  const style: React.CSSProperties = {
    transform: `translate3d(${coordinates.x}px, ${coordinates.y}px, 0)`,
  }

  return (
    <button
      ref={handleRef}
      style={style}
      className={`chat-launcher${isDragging ? ' is-dragging' : ''}`}
      aria-label={isOpen ? t.chat.launcherCloseAria : t.chat.launcherOpenAria}
      aria-expanded={isOpen}
      aria-controls={isOpen ? 'chat-panel' : undefined}
      onClick={onToggle}
      {...listeners}
      {...attributes}
    >
      <MessageCircle size={26} strokeWidth={2.2} />
      <span>{t.chat.launcher}</span>
    </button>
  )
}
