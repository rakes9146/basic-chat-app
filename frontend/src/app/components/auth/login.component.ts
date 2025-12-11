import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  loading = false;
  submitted = false;
  error = '';

  constructor(
    private formBuilder: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.loginForm = this.formBuilder.group({
      userName: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  ngOnInit(): void {}

  get f() {
    return this.loginForm.controls;
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = '';

    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    const userName = this.f['userName'].value;
    const password = this.f['password'].value;

    this.authService.login(userName, password).subscribe({
      next: (response: any) => {
        if (response === true) {
          // Login successful, now fetch user details
          this.authService.getUserByUserName(userName).subscribe({
            next: (user: any) => {
              this.authService.setCurrentUser(user);
              this.router.navigate(['/chat']);
              this.loading = false;
            },
            error: (err: any) => {
              this.error = 'Failed to fetch user details';
              this.loading = false;
            }
          });
        } else {
          this.error = 'Invalid username or password';
          this.loading = false;
        }
      },
      error: (err: any) => {
        this.error = 'Invalid username or password';
        this.loading = false;
      }
    });
  }
}
