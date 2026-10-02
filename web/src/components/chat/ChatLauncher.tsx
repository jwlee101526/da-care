import { useDraggable } from '@dnd-kit/core'
import { MessageCircle } from 'lucide-react'
import { useLanguage } from '../../context/LanguageContext'
import type { Coordinates } from './types'
import { cn } from '@/lib/utils'

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
      data-chat-launcher
      className={cn(
        'fixed right-6 bottom-6 z-[35] flex min-h-[58px] cursor-grab touch-none items-center gap-2.5 rounded-[999px] border-0 bg-[#2455d9] px-[22px] py-3.5 text-[16px] font-bold tracking-[-0.01em] text-white shadow-[0_10px_28px_#2455d938] select-none hover:bg-brand-hover active:cursor-grabbing active:shadow-[0_14px_36px_#2455d952]',
        isDragging && 'cursor-grabbing shadow-[0_14px_36px_#2455d952]',
      )}
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
