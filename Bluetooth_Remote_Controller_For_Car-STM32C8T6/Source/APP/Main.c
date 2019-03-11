#include "Main.h"
#include "stm32f10x_rcc.h"
#include "stm32f10x_gpio.h"
#include "UART1.h"
#include "UART2.h"
#include "delay.h"
#include "DIO.h"
#include "DMREG.h"
#include "string.h"
typedef struct yidong
{
unsigned char zuolun[10];
unsigned char youlun[10];
}CL;//���ֽṹ��

typedef struct chanzi
{
unsigned char zqd[10];//��� ǰ�� ���
unsigned char yqd[10];

}CZ;  //���ӽṹ��
unsigned char zj[2]="ZJ";//��תһ���Ƕ�
unsigned char st[2]="ST";//ֹͣ
unsigned char zz[2]="ZZ";//��ת
unsigned char yz[2]="YZ";//��ת
unsigned char qx[2]="QJ";//ǰ�� Ҳ����ֱ��
unsigned char ks[2]="KS";//��ʼ  Ҳ���ǵ�̨
CL go={{0xff,0x02,0x04,0x2c,0x06},{0xff,0x02,0x06,0x2c,0x06}};//��ǰ�ƶ�
CL stop={{0xff,0x02,0x04,0xc8,0x05},{0xff,0x02,0x06,0xc8,0x05}};//ͣ��
CL right={{0xff,0x02,0x04,0xc8,0x05},{0xff,0x02,0x06,0x2c,0x06}};
CL left={{0xff,0x02,0x04,0x2c,0x06},{0xff,0x02,0x06,0xc8,0x05}};
CL back={{0xff,0x02,0x04,0xdc,0x05},{0xff,0x02,0x05,0xdc,0x05}};
CZ up1={{0xff,0x02,0x00,0xdc,0x05},{0xff,0x02,0x01,0xdc,0x05}};//�������
CZ down1={{0xff,0x02,0x00,0xc0,0x03},{0xff,0x02,0x01,0x20,0x08}};//���²���
CZ ping1={{0xff,0x02,0x00,0xc6,0x04},{0xff,0x02,0x01,0xf1,0x06}};
CZ up2={{0xff,0x02,0x02,0xdc,0x05},{0xff,0x02,0x03,0xdc,0x05}};//�������
CZ down2={{0xff,0x02,0x02,0xc0,0x03},{0xff,0x02,0x03,0x20,0x08}};//���²���
CZ ping2={{0xff,0x02,0x02,0xc6,0x04},{0xff,0x02,0x03,0xf1,0x06}};
CL temp;//���� �ݴ�ֵ
CZ cztemp;// ���� �ݴ�ֵ
char unsigned buff[10];//���ջ�����
char rx_flag;//���ձ�־λ

void ioinit()
{

  GPIO_InitTypeDef GPIO_InitStructure;
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_USART1 | RCC_APB2Periph_GPIOB, ENABLE);
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_11;
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_Out_PP;
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
	GPIO_Init(GPIOB, &GPIO_InitStructure);    

	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_10;
	GPIO_Init(GPIOB, &GPIO_InitStructure);

	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_1;
	GPIO_Init(GPIOB, &GPIO_InitStructure);
	
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_0;
	GPIO_Init(GPIOB, &GPIO_InitStructure);
	
		GPIO_InitStructure.GPIO_Pin = GPIO_Pin_3;
	GPIO_Init(GPIOB, &GPIO_InitStructure);

	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_4;
	GPIO_Init(GPIOB, &GPIO_InitStructure);
	
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_5;
	GPIO_Init(GPIOB, &GPIO_InitStructure);
	
		GPIO_InitStructure.GPIO_Pin = GPIO_Pin_6;
	GPIO_Init(GPIOB, &GPIO_InitStructure);

}

//void right_front(char a)
//{
//	if(a=='g')
//	{
//  GPIO_SetBits(GPIOB,GPIO_Pin_6);
//	GPIO_ResetBits(GPIOB,GPIO_Pin_5);
//	}
//	else if(a=='b')
//	{
//	GPIO_SetBits(GPIOB,GPIO_Pin_5);
//	GPIO_ResetBits(GPIOB,GPIO_Pin_6);
//	}
//	
//		else if(a=='s')
//	{
//	GPIO_SetBits(GPIOB,GPIO_Pin_5);
//	GPIO_SetBits(GPIOB,GPIO_Pin_6);
//	}
//}
//void left_front(char a)
//{
//	if(a=='g')
//	{
//	GPIO_SetBits(GPIOB,GPIO_Pin_3);
//	GPIO_ResetBits(GPIOB,GPIO_Pin_4);
//	}
//	else if(a=='b')
//	{
//	GPIO_SetBits(GPIOB,GPIO_Pin_4);
//	GPIO_ResetBits(GPIOB,GPIO_Pin_3);
//	}
//		else if(a=='s')
//	{
//	GPIO_SetBits(GPIOB,GPIO_Pin_3);
//	GPIO_SetBits(GPIOB,GPIO_Pin_4);
//	}
//}

void left_behind(char a)
{
	if(a=='g')
	{
  GPIO_SetBits(GPIOB,GPIO_Pin_1);
	GPIO_ResetBits(GPIOB,GPIO_Pin_0);
	}
	else if(a=='b')
	{
	GPIO_SetBits(GPIOB,GPIO_Pin_0);
	GPIO_ResetBits(GPIOB,GPIO_Pin_1);
	}
		else if(a=='s')
	{
	GPIO_SetBits(GPIOB,GPIO_Pin_1);//b1= 1
	GPIO_SetBits(GPIOB,GPIO_Pin_0);
	}
}

void right_behind(char a)
{
	if(a=='g')
	{
  GPIO_SetBits(GPIOB,GPIO_Pin_11);
	GPIO_ResetBits(GPIOB,GPIO_Pin_10);
	}
	else if(a=='b')
	{
	GPIO_SetBits(GPIOB,GPIO_Pin_10);
	GPIO_ResetBits(GPIOB,GPIO_Pin_11);
	}
	else if(a=='s')
	{
	GPIO_SetBits(GPIOB,GPIO_Pin_10);
	GPIO_SetBits(GPIOB,GPIO_Pin_11);
	}
}
void hout()
{
	right_behind('b');
	left_behind('b');
//	right_front('b');
//	left_front ('b');

}

void qianj()

{
	right_behind('g');
	left_behind('g');
	

}
void you()

{
	right_behind('g');
	left_behind('b');
//	right_front('b');
//	left_front ('g');

}
void zuo()

{
	right_behind('b');
	left_behind('g');
//	right_front('g');
//	left_front ('b');
}

void xiezuoqian()
{
	right_behind('s');
	left_behind('g');
//	right_front('g');
//	left_front ('s');
}
void xiezuohou()

{
	right_behind('b');
	left_behind('s');
//	right_front('s');
//	left_front ('b');
}

void xieyouhou()

{
	right_behind('s');
	left_behind('b');
//	right_front('b');
//	left_front ('s');
	
	
}
void xieyouqian()
{
	right_behind('g');
	left_behind('s');
//	right_front('s');
//	left_front ('g');
}

void stop1(){
	right_behind('s');
	left_behind('s');
}
	
void UART2_Put_StringL(unsigned char *Str,unsigned char len)
{
	unsigned char i = 0;
	for (i=0;i<len;i++) UART2_Put_Char(*(Str + i));
}



//**********�����鼰���ֶ�����ٶȽǶ����ö��ڡ�DMREG.h��ͷ�ļ�����******//
//**********ֱ�Ӱ��������Ʒ���UART_DM_ReportData()��������Ϳ����ˣ�����UART_DM_ReportData(DM0_Speed1_Position_90)********//

int main(void)
{  		
	char xunflag=1;
	char bianyuan=0;
	char zuoce=0;
	char youce=0;
	char qiance=0;
	SysTick_init(72,10);
	Initial_UART1(9600);            //���ڲ�����9600
	delay_ms(1000);
	Initial_UART2(9600);
  delay_ms(5000);
	ioinit();
	while(1)
	{	
		GPIO_SetBits(GPIOB,GPIO_Pin_6);
	  GPIO_ResetBits(GPIOB,GPIO_Pin_5);
		if(rx_flag==1)
		{
		   rx_flag=0;
			if(buff[0]=='w')
			  qianj();
			if(buff[0]=='s')
				hout();
			if(buff[0]=='a')
				zuo();
			if(buff[0]=='d')
				you();
			if(buff[0]=='t')
				stop1();

		}

				delay_ms(500);
			}
	
	}
			
	
	
	
		
		

