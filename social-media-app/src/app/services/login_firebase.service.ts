import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { getAuth, signInWithCustomToken } from "firebase/auth";

interface TokenResponse {
  token: string;
}

@Injectable({
    providedIn: 'root'
})
export class LoginFirebaseService {
    constructor(private http: HttpClient) { }

    async loginFirebase() {
        const token = await this.http.post<TokenResponse>('/firebase/token', {}).toPromise();

        if (!token || !token.token) {
          throw new Error('Invalid token response');
        }

        const auth = getAuth();
        await signInWithCustomToken(auth, token.token);
    }

}