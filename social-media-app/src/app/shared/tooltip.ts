import { Directive, ElementRef, Input, AfterViewInit } from '@angular/core';

declare var bootstrap: any;

@Directive({
  selector: '[appTooltip]',
  standalone: true
})
export class TooltipDirective implements AfterViewInit {

  @Input('appTooltip') title!: string;
  @Input() placement: string = 'bottom';

  constructor(private el: ElementRef) {}

  ngAfterViewInit(): void {
    const element = this.el.nativeElement;

    // set title động
    element.setAttribute('title', this.title);

    new bootstrap.Tooltip(element, {
      placement: this.placement,
      trigger: 'hover',
      container: 'body',
      animation: true,
      delay: { show: 200, hide: 100 }
    });
  }
}