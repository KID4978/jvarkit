/*
The MIT License (MIT)

Copyright (c) 2015 Pierre Lindenbaum

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


History:
* 2015 creation

*/
package com.github.lindenb.jvarkit.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.lindenb.jvarkit.io.IOUtils;

import htsjdk.variant.vcf.VCFHeader;
import htsjdk.variant.vcf.VCFHeaderLine;


public class Pedigree
	{
	private static final org.slf4j.Logger LOG = com.github.lindenb.jvarkit.util.log.Logging.getLog(Pedigree.class);
	private Map<String,FamilyImpl > families=new TreeMap<String, Pedigree.FamilyImpl>();
	
	public enum Status
		{
		missing,unaffected,affected;
		public int intValue()
			{
			switch(this)
				{
				case missing: return -9;
				case unaffected: return 0;
				case affected: return 1;
				default:throw new IllegalStateException();
				}
			}
		}
	
	public enum Sex
		{
		male,female,unknown;
		public int intValue()
			{
			switch(this)
				{
				case male: return 1;
				case female: return 2;
				case unknown: return 0;
				default:throw new IllegalStateException();
				}
			}
		
		}
	
	public interface Family extends Comparable<Family>
		{
		public String getId();
		public Person getPersonById(String s);
		public java.util.Collection<? extends Person> getIndividuals();
		public Family validate() throws IllegalStateException;
		@Override
		default int compareTo(final Family o) {
			return this.getId().compareTo(o.getId());
			}
		}
	
	private class FamilyImpl
		implements Family
		{
		private String id;
		private Map<String,PersonImpl> individuals=new TreeMap<String,PersonImpl>();
		
		@Override
		public String getId()
			{
			return this.id;
			}
		@Override
		public Person getPersonById(String s)
			{
			return individuals.get(s);
			}
		@Override
		public java.util.Collection<? extends Person> getIndividuals()
			{
			return this.individuals.values();
			}
		@Override
		public int hashCode() {
			return id.hashCode();
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			
			final FamilyImpl other = (FamilyImpl) obj;
			return id.equals(other.id);
			}
		@Override
		public String toString() {
			return this.id;
			}
		
		@Override
		public Family validate() throws IllegalStateException {
			for(final PersonImpl p: this.individuals.values()) {
				p.validate();
				}
			return this;
			}
		
		}
	
	public interface Person
		extends Comparable<Person>
		{
		public String getId();
		public Family getFamily();
		public boolean hasFather();
		public Person getFather();
		public boolean hasMother();
		public Person getMother();
		public Sex getSex();
		public boolean isMale();
		public boolean isFemale();
		public Status getStatus();
		/** return i-th parent 0=father 1=mother */
		public Person getParent( int zeroOrOne);
		public Person validate() throws IllegalStateException;
		
		public boolean isAffected();
		public boolean isUnaffected();
		
		@Override
		default int compareTo(final Person o) {
			int i= getFamily().compareTo(o.getFamily());
			if(i!=0) return i;
			return this.getId().compareTo(o.getId());
			}
		
		}
	
	
	private class PersonImpl
	implements Person
		{
		FamilyImpl family;
		String id;
		String fatherId=null;
		String motherId=null;
		Sex sex=Sex.unknown;
		Status status=Status.unaffected;
		
		@Override
		public Family getFamily()
			{
			return this.family;
			}
		
		@Override
		public String getId()
			{
			return this.id;
			}
		
		@Override
		public boolean isMale() { return Sex.male.equals(this.getSex());}
		@Override
		public boolean isFemale() { return Sex.female.equals(this.getSex());}

		@Override
		public boolean isAffected() { return Status.affected.equals(this.getStatus());}
		@Override
		public boolean isUnaffected() { return Status.unaffected.equals(this.getStatus());}

		
		
		private Person getParent(final String s)
			{
			if(s==null || s.isEmpty() || s.equals("0")) return null;
			return getFamily().getPersonById(s);
			}
		
		public Person getParent( int zeroOrOne) {
			switch(zeroOrOne) {
			case 0: return getFather();
			case 1: return getMother();
			default: throw new IllegalArgumentException("0 or 1 but got "+zeroOrOne);
			}
		}
		
		private boolean hasParent(final String s)
			{
			return !(s==null || s.isEmpty() || s.equals("0"));
			}
		
		@Override
		public boolean hasFather() {
			return hasParent(this.fatherId);
		}
		
		@Override
		public boolean hasMother() {
			return hasParent(this.motherId);
		}
		
		public Person getFather()
			{
			return getParent(fatherId);
			}
		
		public Person getMother()
			{
			return getParent(motherId);
			}
		
		@Override
		public Status getStatus()
			{
			return status;
			}
		
		@Override
		public Sex getSex()
			{
			return sex;
			}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + family.hashCode();
			result = prime * result + id.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			final PersonImpl p =(PersonImpl)obj;
			return	this.family.equals(p.family) &&
					this.id.equals(p.id);
			}

		@Override
		public Person validate() throws IllegalStateException {
			if(this.fatherId!=null && !this.fatherId.equals("0")) {
				final PersonImpl parent = this.family.individuals.get(this.fatherId);
				if(parent==null) throw new IllegalStateException(
					"Individual "+this.toString()+" has father "+this.fatherId+" "+
					"but he is missing in family."
					);
				if(parent.sex == Sex.female) throw new IllegalStateException(
					"Individual "+this.toString()+" has father "+this.fatherId+" "+
					"but he is declared as a woman."
					);
			}
			if(this.motherId!=null && !this.motherId.equals("0")) {
				final PersonImpl parent = this.family.individuals.get(this.motherId);
				if(parent==null) throw new IllegalStateException(
					"Individual "+this.toString()+" has mother "+this.motherId+" "+
					"but she is missing in family."
					);
				if(parent.sex == Sex.male) throw new IllegalStateException(
					"Individual "+this.toString()+" has mother "+this.motherId+" "+
					"but she is declared as a man."
					);
			}
			return this;
		}
		
		@Override
		public String toString() {
			return family+":"+this.id;
			}
		}
	
	private Pedigree()
		{
		
		}
	
	public boolean isEmpty() {
		return this.families.isEmpty();
	}
	
	/** utility function for vcf, returns true if all person's ID in the pedigree
	 * are unique (no same ID shared by two families 
	 */
	public boolean verifyPersonsHaveUniqueNames() {
		final Set<String> m = new HashSet<String>();
		for(final Family f:families.values())
			{
			for(final Person p:f.getIndividuals()) {
				if(m.contains(p.getId())) {
					return false;
					}
				m.add(p.getId());
				}
			}
		return true;
		}
	
	/** utility function for vcf, return a Map<person.id,Person>
	 * will throw an illegalState if two individual have the same ID (but ! families )
	 * @return Map<person.id,Person>
	 */
	public Map<String, Person> getPersonsMap() {
		final Map<String, Person> m = new TreeMap<String, Person>();
		for(final Family f:families.values())
			{
			for(final Person p:f.getIndividuals()) {
				final Person prev = m.get(p.getId());
				if(prev!=null) {
					throw new IllegalStateException(
						"Cannot create a Map<String, Person> because "+prev+" and "+p + " share the same ID : "+p.getId()	
						);
					}
				m.put(p.getId(), p);
				}
			}
		return m;
	}
	
	/** validate pedigree */
	public Pedigree validate() throws IllegalStateException {
		for(final Family f: getFamilies()) f.validate();
		return this;
		}

	
	/** get all the families in this pedigree */
	public java.util.Collection<? extends Family> getFamilies()
		{
		return this.families.values();
		}
	
	public Family getFamilyById(final String famId) {
		return this.families.get(famId);
	}
	
	/** get all the individuals in this pedigree */
	public java.util.Set<Person> getPersons()
		{
		final java.util.Set<Person> set = new HashSet<>();
		for(final Family f:families.values())
			{
			set.addAll(f.getIndividuals());
			}
		return set;
		}
	
	/** get affected individuals */
	public java.util.Set<Person> getAffected()
		{
		return getPersons().stream().filter(P->P.getStatus()==Status.affected).collect(Collectors.toSet());
		}

	/** get unaffected individuals */
	public java.util.Set<Person> getUnaffected()
		{
		return getPersons().stream().filter(P->P.getStatus()==Status.unaffected).collect(Collectors.toSet());
		}
	private void read(final String tokens[])
		{
		final String fam= tokens[0];
		final String indi = tokens[1];
		final String father = tokens[2];
		final String mother = tokens[3];
		final String sex = (tokens.length>4?tokens[4]:"");
		final String status = (tokens.length>5?tokens[5]:"");
		build(fam,indi,father,mother,sex,status);
		}
	
	private void build(final String famId,final String indiId,final String fatherId,final String motherId,final String sexxx,final String status)
		{
		FamilyImpl fam=this.families.get(famId);
		if(fam==null)
			{
			fam=new FamilyImpl();
			fam.id = famId;
			this.families.put(famId, fam);
			}
		if(fam.getPersonById(indiId)!=null) throw new IllegalArgumentException("duplicate individual: "+String.join(" ; ", famId,indiId,fatherId,motherId,sexxx,status));
		final PersonImpl p=new PersonImpl();
		p.family=fam;
		p.id=indiId;
		p.fatherId=fatherId;
		p.motherId=motherId;
		
		if(sexxx!=null)
			{
			if(sexxx.equals("1")) p.sex=Sex.male;
			else if(sexxx.equals("2")) p.sex=Sex.female;
			}
		
		if(status!=null)
			{
			if(status.equals("1")) p.status=Status.affected;
			else if(status.equals("0")) p.status=Status.unaffected;
			}
		
		fam.individuals.put(p.id, p);		
		}
	
	private void read(final BufferedReader r) throws IOException
		{
		final Pattern tab = Pattern.compile("[\t]");
		String line;
		while((line=r.readLine())!=null)
			{
			if(line.isEmpty() || line.startsWith("#")) continue;
			read(tab.split(line));
			}
		}
	
	public static Pedigree readPedigree(final File f) throws IOException
		{
		final BufferedReader r= IOUtils.openFileForBufferedReading(f);
		final Pedigree ped=Pedigree.readPedigree(r);
		r.close();
		return ped;
		}	
	public static Pedigree readPedigree(final BufferedReader r) throws IOException
		{
		final Pedigree ped=new Pedigree();
		ped.read(r);
		return ped;
		}	
	
	public static final String VcfHeaderKey="Sample";
	public static Pedigree readPedigree(final VCFHeader header) {
		return readPedigree(header.getMetaDataInInputOrder());
		}
	/** should be readPedigree(header.getMetaDataInInputOrder()) */
	public static Pedigree readPedigree(final Collection<VCFHeaderLine> metadata) {
		final Pattern comma = Pattern.compile("[,]");
		final Pedigree ped=new Pedigree();
		for(final VCFHeaderLine h:metadata) {
			final String key = h.getKey();
			if(!VcfHeaderKey.equals(key)) continue;
			final String value =h.getValue();
			if(!value.startsWith("<")) {
				LOG.warn("in "+VcfHeaderKey+" value doesn't start with '<' "+value);
				continue;
			}
			if(!value.endsWith(">")) {
				LOG.warn("in "+VcfHeaderKey+" value doesn't end with '>' "+value);
				continue;
			}
			
			String familyId=null;
			String indiId=null;
			String fatherId=null;
			String motherId=null;
			String sexx=null;
			String status=null;

			for(final String t:comma.split(value.substring(1, value.length()-1))) {
				final int eq = t.indexOf("=");
				if(eq==-1) 
					{
					LOG.warn("'=' missing in "+t+" of "+value);
					continue;
					}
				final String left = t.substring(0,eq);
				if(left.equals("Family")) {
					if(familyId!=null) throw new IllegalArgumentException("Family defined twice in " +value);
					familyId= t.substring(eq+1).trim();
					}
				else if(left.equals("ID")) {
					if(indiId!=null) throw new IllegalArgumentException("ID defined twice in " +value);
					indiId= t.substring(eq+1).trim();
					}
				else if(left.equals("Father")) {
					if(fatherId!=null) throw new IllegalArgumentException("fatherId defined twice in " +value);
					fatherId= t.substring(eq+1).trim();
					}
				else if(left.equals("Mother")) {
					if(motherId!=null) throw new IllegalArgumentException("mother defined twice in " +value);
					motherId= t.substring(eq+1).trim();
					}
				else if(left.equals("Sex")) {
					if(sexx!=null) throw new IllegalArgumentException("sex defined twice in " +value);
					sexx= t.substring(eq+1).trim();
					}
				else if(left.equals("Status")) {
					if(status!=null) throw new IllegalArgumentException("status defined twice in " +value);
					status= t.substring(eq+1).trim();
					}
				}
			if(familyId==null) throw new IllegalArgumentException("Family undefined  in " +value);
			if(indiId==null) throw new IllegalArgumentException("ID undefined in " +value);
			ped.build(familyId,indiId,fatherId,motherId,sexx,status);
			}
		return ped;
		}
	
	public Set<VCFHeaderLine> toVCFHeaderLines()  {
		final Set<VCFHeaderLine> set = new LinkedHashSet<>();
		for(final Family f:families.values())
			{
			for(final Person p:f.getIndividuals()) {
				final StringBuilder sb=new StringBuilder();
				sb.append("<Family=");
				sb.append(f.getId());
				sb.append(",ID=");
				sb.append(p.getId());
				sb.append(",Father=");
				sb.append(p.getFather()==null?"0":p.getFather().getId());
				sb.append(",Mother=");
				sb.append(p.getMother()==null?"0":p.getMother().getId());
				sb.append(",Sex=");
				sb.append(p.getSex().intValue());
				sb.append(",Status=");
				sb.append(p.getStatus().intValue());
				sb.append(">");
				set.add(new VCFHeaderLine(VcfHeaderKey, sb.toString()));
				}
			}
		
		return set;
		}
	}
