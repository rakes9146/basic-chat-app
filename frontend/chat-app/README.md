# Chat Application Frontend

This is an Angular-based chat application frontend that supports Node 20.

## Features

- **User Registration**: Create new account with username, password, first name, and last name
- **User Login**: Authenticate existing users
- **Real-time Messaging**: Send and receive messages with delivery status
- **Message Status**: 
  - Single tick (✓) - Message sent
  - Double tick (✓✓) - Message delivered
  - Double blue tick (✓✓) - Message read
- **Conversation Management**: View all conversations and select one to chat
- **Responsive Design**: Works on desktop and mobile devices

## Prerequisites

- Node.js 20.x or higher
- npm 10.x or higher

## Installation

1. Navigate to the project directory:
   ```bash
   cd chat-app
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

## Development Server

Run the development server:
```bash
npm start
```

Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

## Building

Build the project for production:
```bash
npm run build:prod
```

The build artifacts will be stored in the `dist/` directory.

## Project Structure

```
src/
├── app/
│   ├── components/
│   │   ├── auth/          # Login and Registration components
│   │   └── chat/          # Chat component
│   ├── services/
│   │   ├── auth.service.ts
│   │   └── message.service.ts
│   ├── models/
│   │   └── chat.model.ts
│   ├── guards/
│   │   └── auth.guard.ts
│   ├── interceptors/
│   │   └── auth.interceptor.ts
│   ├── app.module.ts
│   ├── app-routing.module.ts
│   ├── app.component.ts
│   └── app.component.html
├── assets/
├── styles.scss
├── main.ts
└── index.html
```

## API Endpoints

The application communicates with the backend at `http://localhost:8080`:

- **POST** `/api/auth/register` - Register new user
- **POST** `/api/auth/login` - Login user
- **GET** `/api/messages/conversations/{userId}` - Get all conversations
- **GET** `/api/messages/conversation/{userId}/{participantId}` - Get messages for a conversation
- **POST** `/api/messages/send` - Send a new message
- **PUT** `/api/messages/{messageId}/status` - Update message status
- **PUT** `/api/messages/markAsRead` - Mark messages as read

## Technologies Used

- **Angular 18**: Frontend framework
- **TypeScript 5.4**: Programming language
- **Bootstrap 5.3**: CSS framework
- **RxJS 7.8**: Reactive programming library
- **Font Awesome 6.5**: Icon library

## Notes

- The application uses local storage to persist user authentication
- All API calls are intercepted to include the authentication token
- Protected routes require authentication via AuthGuard
