package peersim.chord;

public class Flag {
	
	public static int sort_flag = 0;  // �Ƿ���Ҫ����chordId����
	
	public static int drop_packet_rate = 0; // ����message�ĸ���
	
	public static int look_ahead_level = 0;  // ��Ҫ��ǰ������ 
	
	public Flag()
	{
	}
	
	public static void set_Sort_Flag()
	{
		sort_flag = 1;
	}
	
	public static void reset_Sort_Flag()
	{
		sort_flag = 0;
	}
}
