//FrioOndas Version 5
// Tiene las funcionalidades del LCD, pulsadores, Termistor y Bluetooth
#include <LiquidCrystal.h>
#include <SoftwareSerial.h>
#include <math.h>

SoftwareSerial btSerial(7,6); //RX,TX

//TERMISTOR (10Kohm)
//Una pata a 5v
//Y la otra pata se divide en: GND+Resistencia y A0
const int TERMISTOR = 0; // PIN A0
//

//LCD
/* Descripcion de cada PIN:
 *  
 *  VSS = GND (Tierra)
 *  VDD = Resistencia + 5v (270 omh o 220 ohm)
 *  V0 = GND (pin PWM)(o Porenciometro)
 *  RS = 8 (Comandos)
 *  RW = GND (para darle valor 0)
 *  E = 6 (Enable)
 *  D0 = (Nada)
 *  D1 = (Nada)
 *  D2 = (Nada)
 *  D3 = (Nada)
 *  D4 = 10
 *  D5 = 11
 *  D6 = 12
 *  D7 = 13
 *  A = 5v (Anodo)
 *  K = GND (Catodo)
 */
//const int V0 = 5;
const int RS = 8;
const int E = 9;
const int D4 = 10;
const int D5 = 11;
const int D6 = 12;
const int D7 = 13;

//BLUETOOTH
// El BT esta configurado con
// Nombre: FRIOONDAS
// Password: 1234
// Modo: 0 (slave)
// 9600 baudios
//
const int RX_Ard = 0; // La salida TX del Bluetooth va en el PIN D0, RX de Arduino
const int TX_Ard = 1; // La salida RX del BT va en el PIN D1, TX de Arduino
const int PowerBT = 10; // PIN D10 para encender el BT
int estadoBT = 0; //0 apagado; 1 encendido

//PULSADORES
const int PulsadorON = 2; // PIN D2
const int PulsadorOFF = 3; // PIN 3
const int RELE = 4; // PIN 4

int estadoON = 0; //0=sin presionar       1=presionado
int estadoOFF = 0; //0=sin presionar       1=presionado
int salida = 0; // 0 = apagado //en este caso el Rele

// Variables propias del programa
int temporizador = 0; // lleva la cuenta del tiempo antes de apagar el frioondas. Obtenido de los pulsadores o de la app.
int menos_uno = -1;
unsigned long tiempo_actual;
unsigned long tiempo_segundo_anterior;
unsigned long tiempo_temperatura_anterior;
unsigned long tiempo_intervalo_seg = 999;
unsigned long tiempo_intervalo_boton= 300;
unsigned long tiempo_intervalo_quince= 15000;
unsigned long tiempo_boton_ON = 0 ;
unsigned long tiempo_boton_OFF = 0;
unsigned long tiempo_BT = 0;
unsigned long tiempo_OK = 0;//aca seteo el tiempo para mostrar el mensaje de OK
unsigned long tiempo_ciudad = 0;
boolean ban_t1 =false;
boolean ban_act_temp_req=false;
int id_ciudad = 0;
const int cant_ciudad = 3;
String ciudad[cant_ciudad] = {"   San Justo    "," G. Laferrere  ","  I. Casanova   "}; // -1001;-1002;-1003

int temperatura = 0; // temperatura actual
int temp_req; // temperatura requerida obtenida de la app
int temp_req_ant;
int valor_analogico;

String datosRecibidos; // el formato de los datos recibidos debe ser:
                      // "TiempoReq;TemperaturaReq\r\n"
                      // donde ambos son numeros enteros separados por un punto y coma y finalizados por \r\n

LiquidCrystal lcd (RS, E, D4, D5, D6, D7);

void setup() {


//  analogWrite(V0, 140); //entre 0 y 255
  pinMode(PulsadorON, INPUT);
  pinMode(PulsadorOFF, INPUT);
  pinMode(RELE, OUTPUT);
  
  lcd.begin(16,2);
  lcd.setCursor(0,0);
  lcd.print("   FrioOndas    ");
  lcd.setCursor(0,1);
  lcd.print(" Inicializando  ");
  delay(4000);  
  temporizador = 0;
  temp_req = -404;
  temp_req_ant = -404;
  Serial.begin(9600);
  btSerial.begin(9600);

  
}

void loop() {
//
  //mostrar el tiempo restante
  tiempo_actual = millis ();

  //imprime tiempo por cada segundo que pasa
  if ( tiempo_actual - tiempo_segundo_anterior > tiempo_intervalo_seg && temporizador >= 0 && temp_req == -404 && id_ciudad == 0 )
  {
    
    if(temporizador > 0)
      adicionarATemporizador(menos_uno);    

    tiempo_segundo_anterior = tiempo_actual;
    imprimirTiempopRest(temporizador);
    if( (temporizador -1) == 0)
           ban_t1 = true;

           if( ban_t1 && (temporizador -1) == -1)
          {
             temporizador = -100;
             ban_t1= false;
        }
  }

    //imprime temperatura requerida, cada vez que la setee
  if ( ( temp_req != temp_req_ant || ban_act_temp_req )&& temporizador == -404 && temp_req >= -20&& temp_req < 100 && id_ciudad == 0 )
  { 
    if( ban_act_temp_req )
        ban_act_temp_req=false;
    temp_req_ant = temp_req;
    imprimirTempReq(temp_req);
  }
  //verificar si alcance la temperatura deseada
  if( temp_req >= -20 && temp_req >= temperatura )    // REVISAR
  {
      temp_req = -100;
      temporizador = -404;
  }

  //imprime ciudad actual por 15 segundos, cada 1 segundo
  if(id_ciudad != 0 && tiempo_actual - tiempo_segundo_anterior > tiempo_intervalo_seg )
  {
    imprimirCiudad( id_ciudad-1 );// imprime la ciudad en la segunda linea
        
    tiempo_segundo_anterior = tiempo_actual;
    tiempo_ciudad = tiempo_ciudad + tiempo_intervalo_seg;
    if( tiempo_ciudad >= tiempo_intervalo_quince )// && temporizador != -100 && temp_req !=-100 )
    {
      id_ciudad=0;
      tiempo_ciudad=0;
      ban_act_temp_req=true;
      
    }
  }
    //imprime mensaje de OK por 15 segundos tiempo_OK
  if ( (temporizador == -100 || temp_req == -100) && tiempo_actual - tiempo_segundo_anterior > tiempo_intervalo_seg )
  { 
    if( temporizador == -100)
        imprimirOK( 0 );// 0 es para tiempo
    if( temp_req == -100)
        imprimirOK( 1 );// 1 es para temperatura
        
    tiempo_segundo_anterior = tiempo_actual;
    tiempo_OK= tiempo_OK + tiempo_intervalo_seg;
    if( tiempo_OK >= tiempo_intervalo_quince )
    {
      temporizador = 0;
      temp_req = -404;
      tiempo_OK=0;
    }
  }


  
  //mostrar la temeperatura actual
  if ( tiempo_actual -  tiempo_temperatura_anterior > tiempo_intervalo_seg )
  {
    valor_analogico = analogRead( TERMISTOR );//Lee el valor del pin analogo 0 y lo mantiene como val
    temperatura = obtenerTemperatura( valor_analogico );//Realiza la conversión del valor analogo a grados Celsius
    tiempo_temperatura_anterior = tiempo_actual;
    imprimirTempAct(temperatura);
  }

  //LOGICA PULSADORES
  estadoON = digitalRead ( PulsadorON );
  estadoOFF = digitalRead ( PulsadorOFF );

  if ((estadoON == HIGH)&& ((estadoOFF == LOW)) && ( tiempo_actual - tiempo_boton_ON > tiempo_intervalo_boton  ))
  {
    adicionarATemporizador(30);
    temp_req = -404;
    temp_req_ant = -404;
    tiempo_boton_ON = tiempo_actual;
   }

  if ((estadoOFF == HIGH) && (estadoON == LOW) &&(tiempo_actual - tiempo_boton_OFF > tiempo_intervalo_boton  ))
  {
    temporizador = 0;
    temp_req = -404;
    temp_req_ant = -404;
    tiempo_boton_OFF = tiempo_actual;
   }

   
  if ((estadoOFF == HIGH)&&(tiempo_actual - tiempo_boton_OFF > tiempo_intervalo_boton  ) && (estadoON == HIGH)&&( tiempo_actual - tiempo_boton_ON > tiempo_intervalo_boton  ) )
  {
      tiempo_boton_ON = tiempo_actual;
      tiempo_boton_OFF = tiempo_actual;
      if(estadoBT == 0)
      {
        imprimirBT();
      
      }
      else
      {
        imprimirBT();
      }      
   }

// Enviar a Bluetooth cada un segundo
// el formato de envio es:
// "TemperaturaActual;TiempoReq;TemperaturaReq\r\n"
// donde todos son numeros enteros, separados por un punto y coma y finalizado  en \r\n
if( tiempo_actual - tiempo_BT > tiempo_intervalo_seg )
{

  enviarDatos();
  tiempo_BT= tiempo_actual;
}
// Recibir de Bluetooth constantemente
if(btSerial.available())
{
  datosRecibidos = btSerial.readStringUntil('\n');
  Serial.println(datosRecibidos);
  validarDatosRecibidos();
  //ahora deberia transformar esos datos
}

//de acuerdo a los settings prende o apaga el RELE
   if( ( temporizador > 0 && temp_req == -404 )||( temporizador == -404 && temp_req < temperatura ) && temp_req != -100 && temporizador != -100)
   {
    digitalWrite(RELE, HIGH);
   }
   else
   {
    digitalWrite(RELE,LOW);    
   }  
}// fin loop







void adicionarATemporizador(int tiempo_adicional){
  if( temporizador == -404 || temporizador == -100 )
  {
      temporizador = tiempo_adicional;
  }
  else
  {
      if(  (temporizador + tiempo_adicional) <= 32767 )
      {  
          temporizador = temporizador + tiempo_adicional ;       
      }
  }
  
}

void adicionarATemperatura(int temp_adicional){

  if(  temp_req == -404 || temp_req == -100)
  {
    temp_req = temperatura - temp_adicional;
  }
  else
     if(  (temp_req - temp_adicional) >= -20 )
        temp_req = temp_req - temp_adicional ;
          
}

void imprimirTempAct(int temp){

// hacer un borrado del renglon
  lcd.setCursor(0,0);
  lcd.print("                ");
//fin borrado del renglon  
  lcd.setCursor(0,0);
  lcd.print("Temper.: ");
  lcd.print(temp);
  lcd.print(" C"); 
}
  
void imprimirTiempopRest(int tiempo){
  int minu=0;
  int segu=0;
// hacer un borrado del renglon
  lcd.setCursor(0,1);
  lcd.print("                ");
//fin borrado del renglon  
  if(tiempo > 59)
  {
    minu = tiempo/60;
    segu = tiempo%60;
  }
  else
  {
    segu = tiempo;  
  }
  
  lcd.setCursor(0,1);
  lcd.print("Tiempo: ");

  if(minu < 100 )
  {
      if(minu < 10 )
      {
        lcd.print(" 0");
      }
      else
      {
        lcd.print(" ");
      }
  }
  
  lcd.print(minu);
  lcd.print(":");

  if(segu < 10 )
  {
     lcd.print("0");
  }
  lcd.print(segu);
}

void imprimirTempReq(int tem_r )
{
  // hacer un borrado del renglon
  lcd.setCursor(0,1);
  lcd.print("                ");
//fin borrado del renglon  
  
  lcd.setCursor(0,1);
  lcd.print("T. Req.: ");
  lcd.print(tem_r); 
  lcd.print(" C");
}

void imprimirCiudad(int id_ciu )
{
  // hacer un borrado del renglon
  lcd.setCursor(0,1);
  lcd.print("                ");
//fin borrado del renglon  
  
  lcd.setCursor(0,1);
  lcd.print(ciudad[id_ciu]);
}

void imprimirOK( int selector )
{
  // hacer un borrado del renglon
  lcd.setCursor(0,1);
  lcd.print("                ");
//fin borrado del renglon  
  
  lcd.setCursor(0,1);
  if( selector== 0 )
      lcd.print("   Tiempo OK   ");
  if( selector== 1 )
      lcd.print("Temperatura OK ");    
}

double obtenerTemperatura(int RawADC) {  
  double temp;
  temp = log(((10240000/RawADC) - 10000));
  temp = 1 / (0.001129148 + (0.000234125 + (0.0000000876741 * temp * temp ))* temp );
  temp = temp - 273.15;// Converierte de Kelvin a Celsius
  //Para convertir Celsius a Farenheith esriba en esta linea: Temp = (Temp * 9.0)/ 5.0 + 32.0; 
  return temp;
} 


void imprimirBT(){

  if(estadoBT== 0)
  {
      lcd.setCursor(0,1);
      lcd.print("  Bluetooth OFF ");    
  }
  else
  {
      lcd.setCursor(0,1);
      lcd.print("  Bluetooth ON  ");
  }
}

//envia datos a Serial (debug) y a BT
void enviarDatos()
{
  Serial.print(temperatura);
  Serial.print(";");
  Serial.print(temporizador);
  Serial.print(";");
  Serial.print(temp_req);
  Serial.println();
  
  btSerial.print(temperatura);
  btSerial.print(";");
  btSerial.print(temporizador);
  btSerial.print(";");
  btSerial.print(temp_req);
  btSerial.println();
}
  

boolean validarDatosRecibidos()
{
  boolean adicionar_tiempo= false;
  boolean adicionar_temperatura= false;
  String primera;
  String segunda;
  int num1; // aqui deberia ir el tiempo requerido
  int num2; // aqui deberia ir la temperatura requerida
  boolean seg = false;
  boolean ignor = false;
  boolean validado=false;
  int j =1 ;
  if( datosRecibidos.length() <= 0 )
    return false;
  for(int i=0; i < datosRecibidos.length(); i++ )
  {
      
    if( i == 0 && datosRecibidos.charAt(i) == '+' )
      adicionar_tiempo=true;
    else
    {
      if( datosRecibidos.charAt(i) == ';' )
        {
          ignor=true;
          j=i+1;
        }
      if( ignor )
        seg=true;
      else
      {
        if( seg )
        {
            if( i == j && datosRecibidos.charAt(i) == '+' )
            {
                adicionar_temperatura=true;
            }
            else
              segunda = segunda + datosRecibidos.charAt(i);
        }
        else  
          primera = primera + datosRecibidos.charAt(i);
      }
      ignor=false;
    }
  }
    //ahora verificar limites
    num1 = primera.toInt();
    num2 = segunda.toInt();
    if( (num1 < -1000) || (num1 >= -1 && num1 < 32767)|| num1 == -404 || num1 == -100 ) // el tiempo requerido puede ser -1 a infinito
      {
            if ( (num2 > -20 && num2 < 100 ) || num2 == -404 || num2 == -100 ) // la temperatura requerida puede ser mayor a -20
              validado=true;
      }
      if ( validado == false )
        return false;
//antes de devolver true, ejecuto las acciones
    if( num1 == -404 || num2 == -404 || num1 < -1000)
    {
      if(adicionar_tiempo) // pregunto si debo adicionar o simplemente setear
      {
        adicionarATemporizador(num1);
        temp_req = num2;
      }
      else
            if(adicionar_temperatura) // pregunto si debo adicionar tiempo o simplemente setear
            {
                temporizador = num1;
                adicionarATemperatura(num2);
            }
            else
            {
                //logica para recibir -1001 en adelante
                if(num1 < -1000)
                {
                    id_ciudad =  ((-1) * num1) - 1000;
                    
                    if( id_ciudad > cant_ciudad ) // si me vino un id incorrecto
                    {
                      id_ciudad = 0;
                    }
                    else
                    {
                      temporizador=-404;
                      temp_req = num2;
                    }
                }
                else
                {
                  temporizador = num1;
                  temp_req = num2;
                }
            }
        
    }
    else
      return false;
    return true;

}

