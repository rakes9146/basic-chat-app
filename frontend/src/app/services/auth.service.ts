import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { User, AuthResponse } from '../models/chat.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private userServiceUrl = `${environment.userServiceUrl}`;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadUserFromLocalStorage();
  }

  register(user: User): Observable<any> {
    return this.http.post<any>(`${this.userServiceUrl}`, user);
  }

  login(userName: string, password: string): Observable<any> {
    const credentials = { userName, password };
    return this.http.post<any>(`${this.userServiceUrl}/login`, credentials);
  }

  getUserByUserName(userName: string): Observable<User> {
    return this.http.get<User>(`${this.userServiceUrl}/${userName}`);
  }

  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.userServiceUrl}`);
  }

  setCurrentUser(user: User): void {
    this.currentUserSubject.next(user);
    localStorage.setItem('currentUser', JSON.stringify(user));
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  logout(): void {
    localStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
  }

  private loadUserFromLocalStorage(): void {
    const user = localStorage.getItem('currentUser');
    if (user) {
      const parsedUser = JSON.parse(user);
      this.currentUserSubject.next(parsedUser);
    }
  }
}
