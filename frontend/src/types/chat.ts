export interface ChatSession {
  id: number;
  participant1Phone: string;
  participant2Phone: string;
  createdAt: string;
  updatedAt: string;
  lastMessageAt?: string;
  lastMessagePreview?: string;
}

export interface ChatMessage {
  id: number;
  chatSession: {
    id: number;
  };
  senderPhone: string;
  messageContent: string;
  sentAt: string;
  isRead: boolean;
  readAt?: string;
}

export interface SendMessageRequest {
  messageContent: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}

export interface ChatSessionWithOtherParticipant extends ChatSession {
  otherParticipant?: {
    phone: string;
    role: string;
  };
}

export interface ChatPanelState {
  isOpen: boolean;
  selectedSessionId?: number;
  sessions: ChatSessionWithOtherParticipant[];
  messages: ChatMessage[];
  unreadCount: number;
  loading: boolean;
  error?: string;
}