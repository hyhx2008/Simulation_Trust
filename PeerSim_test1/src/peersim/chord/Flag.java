package peersim.chord;

public class Flag {
	
	public static int sort_flag = 0;  // �Ƿ���Ҫ����chordId����
	
	public static double drop_packet_rate = 0.8; // ����ڵ㶪��message�ĸ���
	
	public static double malicious_node_rate = 0.4; // �ж��ٶ���ڵ�
	
	public static int look_ahead_level = -1;  // ��Ҫ��ǰ������  -1=off 0=0-level 1=1-level 2=2-level
	
	public static int K = 3; //0-ֻȥһ��finger��1-ȡ����finger���ŵ��Ǹ���2-3��finger���Դ�����  
	
	public Flag(){}
	
	public static void set_Sort_Flag()
	{
		sort_flag = 1;
	}
	
	public static void reset_Sort_Flag()
	{
		sort_flag = 0;
	}
}
