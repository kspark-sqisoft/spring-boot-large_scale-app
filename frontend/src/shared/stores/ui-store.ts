import { create } from 'zustand'

type UiStore = {
  boardTitle: string
  setBoardTitle: (title: string) => void
  lastFormMessage: string | null
  setLastFormMessage: (message: string | null) => void
}

export const useUiStore = create<UiStore>((set) => ({
  boardTitle: '기술 게시판',
  setBoardTitle: (boardTitle) => set({ boardTitle }),
  lastFormMessage: null,
  setLastFormMessage: (lastFormMessage) => set({ lastFormMessage }),
}))
