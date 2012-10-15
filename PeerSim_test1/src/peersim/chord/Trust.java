package peersim.chord;

public class Trust {
	
	private long pfm_pos;
	private long pfm_neg;
	
	private long pfm_total;
	
	private long blf_pos;
	private long blf_neg;
	
	private long blf_total;
	
	public Trust()
	{
		this.pfm_pos = 1;
		this.pfm_neg = 1;
		
		this.pfm_total = pfm_pos + pfm_neg;
		
		this.blf_pos = 1;
		this.blf_neg = 1;
		this.blf_total = blf_pos + blf_neg;
	}
	
	public void increase_pfm_pos()
	{
		this.pfm_pos++;
		this.pfm_total++;
	}
	
	public void increase_pfm_neg()
	{
		this.pfm_neg++;
		this.pfm_total++;
	}
	
	public void increase_blf_pos()
	{
		this.blf_pos++;
		this.blf_total++;
	}
	
	public void increase_blf_neg()
	{
		this.blf_neg++;
		this.blf_total++;
	}
	
	public double get_trust_in_pfm()
	{
		return 1.0*pfm_pos/pfm_total;
	}
	
	public double get_trust_in_blf()
	{
		return 1.0*blf_pos/blf_total;
	}
}
