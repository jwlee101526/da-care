import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const variants = cva(
  'inline-flex items-center justify-center gap-6 min-h-12 px-5 py-3 rounded-lg border border-transparent text-[15px] font-[550] leading-[1.4] transition-[background-color] duration-150',
  {
    variants: {
      variant: {
        primary: 'bg-brand text-white hover:bg-brand-hover',
        secondary: 'bg-white border-[#d9dfe8] text-[#303946] hover:bg-[#f4f6f9]',
        plain: '',
      },
      size: {
        default: '',
        compact: 'min-h-11 gap-3 px-4 py-2.5 text-[14px] leading-[1.4] max-[600px]:gap-2 max-[600px]:px-3',
      },
    },
    defaultVariants: { variant: 'plain', size: 'default' },
  },
)

export type ButtonVariantProps = VariantProps<typeof variants>

/**
 * Link나 a 태그에도 버튼 모양을 입힐 때 쓴다. className은 기본 클래스와 충돌하면 덮어쓴다.
 * 글자 크기를 바꾸면 tailwind-merge가 leading을 지우므로 leading도 함께 넘긴다.
 */
export function buttonVariants({ variant, size, className }: ButtonVariantProps & { className?: string } = {}) {
  return cn(variants({ variant, size }), className)
}
