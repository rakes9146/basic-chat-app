import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit {
  registerForm: FormGroup;
  loading = false;
  submitted = false;
  error = '';
  success = '';

  constructor(
    private formBuilder: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.registerForm = this.formBuilder.group({
      userName: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]]
    }, {
      validators: this.passwordMatchValidator()
    });
  }

  ngOnInit(): void {}

  get f() {
    return this.registerForm.controls;
  }

  passwordMatchValidator() {
    return (formGroup: FormGroup) => {
      const password = formGroup.get('password');
      const confirmPassword = formGroup.get('confirmPassword');

      if (password && confirmPassword && password.value !== confirmPassword.value) {
        confirmPassword.setErrors({ 'passwordMismatch': true });
      }
    };
  }

  onSubmit(): void {
    this.submitted = true;
    this.error = '';

    if (this.registerForm.invalid) {
      return;
    }

    this.loading = true;
    const user = {
      userName: this.f['userName'].value,
      password: this.f['password'].value,
      firstName: this.f['firstName'].value,
      lastName: this.f['lastName'].value,
      email: this.f['email'].value
    };

    this.authService.register(user).subscribe({
      next: (response: any) => {
        // Check if response indicates success
        if (response && (response.success === true || response.success === undefined)) {
          this.success = response.message || 'Registration successful! Redirecting to login...';
          this.loading = false;
          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 2000);
        } else {
          // Backend returned a response but with success: false
          this.error = response.message || 'Registration failed';
          this.loading = false;
        }
      },
      error: (err: any) => {
        console.error('Registration error:', err);
        this.error = err.error?.message || 'An error occurred during registration. User might already exist.';
        this.loading = false;
      }
    });
  }
}
