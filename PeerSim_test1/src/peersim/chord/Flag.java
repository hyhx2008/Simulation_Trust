package peersim.chord;

public class Flag {
	
	public static int sort_flag = 0;
	
	public Flag()
	{
		sort_flag = 0 ;
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
