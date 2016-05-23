David
LCD420.C
Last week
Atom Zycreated and shared an item in
Thu. 7:10 pm
Google Drive Folder
ProjetCodeC (1)
Created items:
C
LCD420.C
Can edit
You
Can edit
François Marchal
Can edit
Nekrozys Ravenheart
No recorded activity before 19 May 2016

///////////////////////////////////////////////////////////////////////////////////////////////
////                             LCD420.C                                                  ////
////            Driver for common 4x20 LCD modules  (Fonction de haut niveau )             ////
////                                                                                       ////
////  lcd_init()   Must be called before any other function.                               ////
////                                                                                       ////
////  lcd_putc(c)  Will display c on the next position of the LCD.                         ////
////                     The following have special meaning:                               ////
////                      \f  Clear display                                                ////
////                      \n  Go to start of second line                                   ////
////                      \b  Move back one position                                       ////
////                                                                                       ////
////  lcd_gotoxy(x,y) Set write position on LCD (upper left is 1,1 -> 20,4)                ////
////                                                                                       ////
////  lcd_getc(x,y)   Returns character at position x,y on LCD                                ////
////                                                                                       ////
///////////////////////////////////////////////////////////////////////////////////////////////
////        (C) Copyright 1996,1997 Custom Computer Services                               ////
//// This source code may only be used by licensed users of the CCS C                      ////
//// compiler.  This source code may only be distributed to other                          ////
//// licensed users of the CCS C compiler.  No other use, reproduction                     ////
//// or distribution is permitted without written permission.                              ////
//// Derivative programs created using this software in object code                        ////
//// form are not restricted in any way.                                                   ////
///////////////////////////////////////////////////////////////////////////////////////////////

// As defined in the following structure the pin connection is as follows:
//     B0  enable
//     B1  rs
//     B2  rw
//     B4  D4
//     B5  D5
//     B6  D6
//     B7  D7
//
//   LCD pins D0-D3 are not used and PIC B3 is not used.

struct lcd_pin_map {                 // This structure is overlayed ( Mettre dans l'ordre de poid faible à fort )
           int     data : 4;         // be pin B0.
           BOOLEAN rs;               // access to the LCD pins.
           BOOLEAN rw;               // The bits are allocated from
           BOOLEAN enable;           // on to an I/O port to gain
           BOOLEAN unused;           // low order up.  ENABLE will

        } lcd;

#byte lcd = 0xF83                        // This puts the entire structure ( se trouve à la page 47 du datasheet dans "SPECIAL FUNCTION REGISTER MAP" )
                                         // on to port B (at address 6)

#define lcd_type 2           // 0=5x7, 1=5x10, 2=2 lines


BYTE const LCD_INIT_STRING[4] = {0x20 | (lcd_type << 2), 0xc, 1, 6};
                             // These bytes need to be sent to the LCD
                             // to start it up.


                             // The following are used for setting
                             // the I/O port direction register.

struct lcd_pin_map const LCD_WRITE = {0,0,0,0,0}; // For write mode all pins are out
struct lcd_pin_map const LCD_READ = {15,0,0,0,0}; // For read mode data pins are in  ( 15 car les 4 bits du lcd doivent etre à l'état haut et du coté gauche car déplacé en premiere ligne dans struct )


BYTE lcdline;

BYTE lcd_read_byte() {  // Fonction de bas niveau
      BYTE low,high;

      set_tris_d(LCD_READ);
      lcd.rw = 1;
      delay_cycles(1);
      lcd.enable = 1;
      delay_cycles(1);
      high = lcd.data;
      lcd.enable = 0;
      delay_cycles(1);
      lcd.enable = 1;
      delay_us(1);
      low = lcd.data;
      lcd.enable = 0;
      set_tris_d(LCD_WRITE);
      return( (high<<4) | low);
}


void lcd_send_nibble( BYTE n ) {  // Fonction de bas niveau
      lcd.data = n;
      delay_cycles(1);
      lcd.enable = 1;
      delay_us(2);
      lcd.enable = 0;
}


void lcd_send_byte( BYTE address, BYTE n ) {  // Fonction de bas niveau

      lcd.rs = 0;
      while ( bit_test(lcd_read_byte(),7) ) ;
      lcd.rs = address;
      delay_cycles(1);
      lcd.rw = 0;
      delay_cycles(1);
      lcd.enable = 0;
      lcd_send_nibble(n >> 4);
      lcd_send_nibble(n & 0xf);
}


void lcd_init() {
    BYTE i;

    set_tris_d(LCD_WRITE);
    lcd.rs = 0;
    lcd.rw = 0;
    lcd.enable = 0;
    delay_ms(15);
    for(i=1;i<=3;++i) {
       lcd_send_nibble(3);
       delay_ms(5);
    }
    lcd_send_nibble(2);
    for(i=0;i<=3;++i)
       lcd_send_byte(0, LCD_INIT_STRING[i]);
}


void lcd_gotoxy( BYTE x, BYTE y) {
   BYTE address;

   switch(y) {
     case 1 : address=0x80;break;
     case 2 : address=0xc0;break;
     case 3 : address=0x94;break;
     case 4 : address=0xd4;break;
   }
   address+=x-1;
   lcd_send_byte(0,address);
}

void lcd_putc( char c) {
   switch (c) {
     case '\f'   : lcd_send_byte(0,1);
                   lcdline=1;
                   delay_ms(2);
                                           break;
     case '\n'   : lcd_gotoxy(1,++lcdline);        break;
     case '\b'   : lcd_send_byte(0,0x10);  break;
     default     : lcd_send_byte(1,c);     break;
   }
}

char lcd_getc( BYTE x, BYTE y) {
   char value;

    lcd_gotoxy(x,y);
    lcd.rs=1;
    value = lcd_read_byte();
    lcd.rs=0;
    return(value);
}

