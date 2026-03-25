import { Injectable } from "@angular/core";
import Swal from 'sweetalert2';
@Injectable({
  providedIn: 'root'
})
export class DevelopingAlert {
    preventDeveloping(event: Event) {
        event.preventDefault();
        event.stopPropagation();
    
        Swal.fire({
          icon: 'info',
          title: 'Đang phát triển',
          text: 'Tính năng này hiện đang được phát triển. Vui lòng quay lại sau nhé!',
          confirmButtonText: 'Đã hiểu',
          confirmButtonColor: '#3085d6',
          timer: 3000,              // tự đóng sau 3 giây (tùy chọn)
          timerProgressBar: true,
          showClass: {
            popup: 'animate__animated animate__fadeInDown'
          },
          hideClass: {
            popup: 'animate__animated animate__fadeOutUp'
          }
        });
      }
}