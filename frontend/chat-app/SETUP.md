# Angular Chat Application - Setup Instructions

## Prerequisites

Before starting, ensure you have:
- **Node.js 20.x** or higher installed
- **npm 10.x** or higher
- A code editor (VS Code recommended)

## Quick Start

### 1. Install Dependencies

Open terminal/PowerShell in the `chat-app` directory and run:

```powershell
npm install
```

This will install all required Angular and Bootstrap dependencies.

### 2. Start Development Server

```powershell
npm start
```

The application will be available at `http://localhost:4200`

The application will automatically reload when you make changes.

## Project Features

### Authentication
- **User Registration**: Create new account with username, password, first name, and last name
- **User Login**: Sign in with credentials
- **Auth Guard**: Protected routes that redirect to login if not authenticated

### Chat Features
- **Conversations List**: View all active conversations
- **Real-time Messaging**: Send and receive messages
- **Message Status Indicators**:
  - ✓ Single tick = Message sent
  - ✓✓ Double tick = Message delivered  
  - ✓✓ (Blue) Double tick = Message read
- **Auto-scroll**: Messages scroll automatically when new messages arrive
- **Responsive Design**: Works on desktop and mobile

## Project Structure

```
chat-app/
├── src/
│   ├── app/
│   │   ├── components/
│   │   │   ├── auth/
│   │   │   │   ├── login.component.ts
│   │   │   │   ├── login.component.html
│   │   │   │   ├── login.component.scss
│   │   │   │   ├── register.component.ts
│   │   │   │   ├── register.component.html
│   │   │   │   └── register.component.scss
│   │   │   └── chat/
│   │   │       ├── chat.component.ts
│   │   │       ├── chat.component.html
│   │   │       └── chat.component.scss
│   │   ├── services/
│   │   │   ├── auth.service.ts
│   │   │   └── message.service.ts
│   │   ├── models/
│   │   │   └── chat.model.ts
│   │   ├── guards/
│   │   │   └── auth.guard.ts
│   │   ├── interceptors/
│   │   │   └── auth.interceptor.ts
│   │   ├── app.module.ts
│   │   ├── app-routing.module.ts
│   │   ├── app.component.ts
│   │   └── app.component.html
│   ├── assets/
│   ├── environments/
│   ├── styles.scss
│   ├── main.ts
│   └── index.html
├── angular.json
├── tsconfig.json
├── package.json
└── README.md
```

## Available Scripts

```bash
# Start development server
npm start

# Build for production
npm run build:prod

# Run unit tests
npm test

# Lint code
npm run lint
```

## Backend Integration

The application connects to backend services at `http://localhost:8080`:

### Required API Endpoints

**Authentication:**
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user

**Messages:**
- `GET /api/messages/conversations/{userId}` - Get all conversations
- `GET /api/messages/conversation/{userId}/{participantId}` - Get messages for conversation
- `POST /api/messages/send` - Send message
- `PUT /api/messages/{messageId}/status` - Update message status
- `PUT /api/messages/markAsRead` - Mark messages as read

## Technologies Used

- **Angular 18**: Modern JavaScript framework
- **TypeScript 5.4**: Typed JavaScript
- **Bootstrap 5.3**: CSS framework
- **RxJS 7.8**: Reactive programming library
- **Font Awesome 6.5**: Icon library
- **Node 20**: JavaScript runtime

## Development Tips

1. **Hot Module Replacement**: Changes to files are automatically detected and reloaded
2. **Console Errors**: Check browser console (F12) for any API errors
3. **Local Storage**: User data is saved in browser's local storage
4. **Authentication Token**: Make sure your backend is providing tokens on login

## Common Issues

### Port 4200 Already in Use
```powershell
# Find and kill the process, or use a different port:
ng serve --port 4201
```

### Node Modules Not Installing
```powershell
# Clear npm cache and reinstall
npm cache clean --force
rm -r node_modules package-lock.json
npm install
```

### API Connection Issues
- Verify backend is running on `http://localhost:8080`
- Check browser console for CORS errors
- Ensure authentication token is being sent in headers

## Building for Production

```powershell
npm run build:prod
```

This creates an optimized production build in the `dist/` directory.

## Support

For issues or questions:
1. Check the browser console for error messages
2. Verify backend services are running
3. Check network tab in browser dev tools for API requests
4. Review the README.md file in the project root
