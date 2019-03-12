/*
 * Copyright 2019 Government of Canada
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package net.refractions.chyf.hygraph;

import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import net.refractions.chyf.ChyfDatastore;
import net.refractions.chyf.hygraph.EFlowpath;
import net.refractions.chyf.pourpoint.Pourpoint;
import net.refractions.chyf.rest.GeotoolsGeometryReprojector;

/**
 * Tests the projection of various pourpoints 
 * and c-codes.
 * 
 * @author Emily
 *
 */
public class PourpointProjectionTest {

	@Rule
	public TestRule rule = BasicTestSuite.SETUP_RULE;
	
	
	@Test
	public void test_FindDownstreamFlowpathsSpecialCases() throws ParseException {
		
		Coordinate[] points = new Coordinate[] {
				//this is a flowpath nexus with a single input and output 
				//and no catchment files
				new Coordinate(-73.16500, 45.42785),
				
		};
		
		String[][] expectedResults = new String[][] {
			{
				"LINESTRING ( -73.160894 45.4279347, -73.1602026 45.4281576, -73.160177 45.4281575, -73.1601259 45.4281484, -73.1601005 45.4281483, -73.1599599 45.4281389, -73.1598193 45.4281385, -73.1597808 45.4281744, -73.1597679 45.4281923, -73.1597677 45.4282193, -73.1597672 45.4283183, -73.159767 45.4283453, -73.1597541 45.4283633, -73.1597283 45.4284081, -73.159728 45.4284442, -73.1597407 45.4284622, -73.1597405 45.4284892, -73.1597272 45.4285881, -73.1597267 45.4286781, -73.1597265 45.4287051, -73.1597259 45.4287951, -73.1597002 45.428831, -73.1595847 45.4289027, -73.1595591 45.4289116, -73.1594564 45.4289922, -73.1594562 45.4290193, -73.1594688 45.4290463, -73.1594814 45.4290643, -73.1595069 45.4290914, -73.159545 45.4291275, -73.1595704 45.4291546, -73.159583 45.4291816, -73.1596207 45.4292987, -73.1596456 45.4293977, -73.1596705 45.4295148, -73.1596829 45.4295688 )"				
			},
		};
		
		for (Pourpoint.CType type : Pourpoint.CType.values()) {
			if (type == Pourpoint.CType.NEAREST_FLOWPATH) continue; //this ccode is currently not supported
			testData(points, expectedResults, type.ccode);
		}
		
	}
	
	@Test
	public void test_FindDownstreamFlowpathsCcodeM2() throws ParseException {
		
		Coordinate[] points = new Coordinate[] {
				new Coordinate(-73.307687, 45.618841),
				new Coordinate(-73.19234842892683, 45.71639428715012),
				new Coordinate(-73.34324545705215, 45.35112971289037),
				
				//bank catchment
				new Coordinate(-73.36910403700568, 45.376500627354744),
				new Coordinate(-73.25782858021114, 45.446277111061534),
				
				//waterbody catchments
				new Coordinate(-73.25984, 45.44595)
		};
		
		String[][] expectedResults = new String[][] {
			{
				"LINESTRING ( -73.3082603 45.6186652, -73.3082344 45.6187102, -73.3081702 45.6187551, -73.3081059 45.6188089, -73.3079392 45.6187997, -73.3078882 45.6187006, -73.3077985 45.6187005, -73.3076959 45.6186913, -73.3076063 45.6186462, -73.307478 45.6186909, -73.3074265 45.6187539, -73.3073366 45.6187897, -73.3072467 45.6188075, -73.3071571 45.6187804, -73.3070292 45.6186812, -73.3069267 45.618681)"				
			},
			{
//				"LINESTRING (-73.1937394 45.719888,-73.1920782 45.7197674)",
				"LINESTRING ( -73.1915596 45.7141684, -73.19156 45.7142399, -73.1915636 45.7143064, -73.1915722 45.7143819, -73.1915722 45.7144505, -73.191577 45.7145084, -73.1915775 45.714592, -73.191578 45.7146865, -73.1915724 45.7147585, -73.1915705 45.7148153, -73.1915614 45.7149514, -73.1915515 45.7150194, -73.1915487 45.7151208, -73.1915364 45.7152122, -73.1915291 45.7153117, -73.191528 45.7154088, -73.191528 45.7155084, -73.1915218 45.7156012, -73.1915244 45.7156672, -73.1915282 45.7157099, -73.1915312 45.7157722, -73.1915376 45.7158118, -73.1915516 45.7159503, -73.1915576 45.7160141, -73.1915646 45.7161032, -73.1915687 45.7161418, -73.1915806 45.7162111, -73.1915951 45.71631, -73.191606 45.7163993, -73.1916189 45.7164624, -73.191629 45.7165117, -73.1916368 45.7165791, -73.1916436 45.7166681, -73.1916514 45.716732, -73.1916569 45.7167822, -73.1916598 45.7168417, -73.1916721 45.7169303, -73.1916764 45.7170188, -73.1916845 45.7170782, -73.1916951 45.7171817, -73.1916987 45.7172898, -73.1917006 45.7173412, -73.1917098 45.7174065, -73.19172 45.7174611, -73.1917305 45.7175635, -73.1917332 45.7176189, -73.1917399 45.7176878, -73.1917504 45.7178378, -73.191749 45.7178833, -73.1917546 45.7179316, -73.1917616 45.7179693, -73.1917653 45.7180477, -73.1917731 45.7181155, -73.1917789 45.7181647, -73.1917929 45.7182412, -73.1918 45.7182955, -73.1918106 45.7183757, -73.1918244 45.7184389, -73.1918345 45.7184809, -73.1918569 45.7185571, -73.1918715 45.7186019, -73.1919031 45.7186985, -73.1919115 45.7187361, -73.1919202 45.7187801, -73.191928 45.7188189, -73.1919437 45.7189212, -73.1919518 45.7189794, -73.1919636 45.7190469, -73.1919774 45.7191177, -73.1920228 45.7193183, -73.1920319 45.7193552, -73.1920395 45.719407, -73.19205 45.7195163, -73.1920628 45.7195826, -73.1920708 45.7196282, -73.1920761 45.7197175, -73.1920782 45.7197674 )"
			},
			{
				"LINESTRING ( -73.343437 45.349449, -73.3435518 45.3494941, -73.3436283 45.3495122, -73.3436282 45.3495842, -73.3436526 45.3499712, -73.3436384 45.350484, -73.3439176 45.3510693, -73.3441712 45.3516457, -73.3444627 45.3523839, -73.3447672 45.3530772, -73.3449574 45.3535094, -73.3450974 45.3536356, -73.3453014 45.3537259, -73.3456331 45.3537983, -73.3460413 45.3538708, -73.3464749 45.3539704, -73.3469213 45.354079, -73.347355 45.3541606, -73.3474315 45.3542056, -73.3475713 45.3543948, -73.3477241 45.354584, -73.3479024 45.3547192, -73.3479659 45.3548182, -73.3479781 45.3550613, -73.3479904 45.3551872, -73.3480411 45.3553223, -73.3481302 45.3554214, -73.3482321 45.3554846, -73.3484106 45.3555657, -73.3484871 45.3556289, -73.3486014 45.355845, -73.3487794 45.3560792, -73.3489957 45.3563224, -73.3491612 45.3564846, -73.3492884 45.3566288, -73.3493904 45.3567099, -73.3495562 45.3567461, -73.3496072 45.3567642, -73.349671 45.3567822, -73.349722 45.3568093, -73.3499004 45.3568726, -73.3501172 45.3569538, -73.3501683 45.3569629, -73.3503214 45.3569451, -73.3506152 45.3568645, -73.3508835 45.3567838, -73.3509601 45.3567659, -73.3511388 45.3567662, -73.3514197 45.3567485, -73.3516495 45.3567128, -73.3517005 45.3567039, -73.3517772 45.356704, -73.3519304 45.3566592, -73.3520326 45.3566503, -73.352224 45.3566416, -73.3523773 45.3566148, -73.3525942 45.3566151, -73.3527218 45.3566332, -73.3527984 45.3566603, -73.3529257 45.3567595, -73.3530023 45.3567866, -73.3530788 45.3567957, -73.3532193 45.3567689, -73.3535129 45.3567332, -73.3536151 45.3567153, -73.3536788 45.3567154, -73.3537428 45.3566975, -73.3538065 45.3566886, -73.3539216 45.3566347, -73.3542535 45.3566262, -73.3545215 45.3566265, -73.3547001 45.3566537, -73.3547766 45.3566809, -73.3548277 45.3566899, -73.3550191 45.3566901, -73.355185 45.3567084, -73.3554658 45.3567267, -73.3556445 45.3567269, -73.3557975 45.3567542, -73.3560527 45.3567995, -73.3561037 45.3568085, -73.3561547 45.3568266, -73.3563206 45.3568628, -73.356614 45.3568991, -73.3567033 45.3569173, -73.3567543 45.3569353, -73.3568054 45.3569444, -73.3569838 45.3570166, -73.3572773 45.357071, -73.3575963 45.3571164, -73.3578897 45.3571617, -73.3581194 45.35718, -73.3584002 45.3571803, -73.35863 45.3571806, -73.3587449 45.3571808, -73.3589235 45.357163, -73.3592172 45.3571274, -73.3594982 45.3570737, -73.3595748 45.3570648, -73.3597279 45.357074, -73.3599066 45.3570652, -73.3600343 45.3570653, -73.3601875 45.3570295, -73.3604813 45.3569579, -73.3606217 45.3569401, -73.3606855 45.3569222, -73.360826 45.3568863, -73.3609666 45.3568236, -73.3611712 45.3566978, -73.361299 45.356635, -73.3614139 45.3566171, -73.3617073 45.3566265, -73.3618604 45.3566447, -73.3620135 45.3566989, -73.3623068 45.3567982, -73.3625746 45.3569065, -73.3626383 45.3569336, -73.3627403 45.3569967, -73.3629189 45.3570599, -73.3629698 45.3570779, -73.3630208 45.357105, -73.3630847 45.3571141, -73.3631994 45.3571592, -73.3632249 45.3571772, -73.3633014 45.3571953 )",
			},
			//bank
			{
				"LINESTRING ( -73.3699599 45.3756773, -73.3701903 45.3757165 )"
			},
			{
				"LINESTRING ( -73.2510517 45.4371473, -73.2517798 45.4362944 )"
			},
			{
				"LINESTRING ( -73.2540555 45.437975, -73.2540899 45.4380044, -73.2541177 45.4380282, -73.2542027 45.4381077, -73.2542448 45.4381445, -73.2543109 45.4382062, -73.2544791 45.4383393, -73.2545748 45.4384144, -73.2546522 45.4385265, -73.2546966 45.4385793, -73.254745 45.4386407, -73.2547836 45.4386862, -73.2548243 45.4387276, -73.2549346 45.438872, -73.2550126 45.4389828, -73.2550867 45.4390894, -73.2551537 45.4391917, -73.2551989 45.439272, -73.2552535 45.4393788, -73.2552987 45.4394853, -73.2553376 45.4395697, -73.2554714 45.4397496, -73.25555 45.4398504, -73.2556322 45.4399747, -73.2556749 45.4400936, -73.255735 45.4402244, -73.2557657 45.440291, -73.2558493 45.4404297, -73.2558881 45.4404693, -73.2560809 45.4406664, -73.2561977 45.4408064, -73.2562628 45.4409105, -73.256381 45.4410905, -73.2563899 45.4411903, -73.2563905 45.4412689, -73.2565917 45.4415881, -73.2568036 45.4418668, -73.2570932 45.4422717, -73.2572242 45.4424433, -73.2573073 45.4425521, -73.2574338 45.4427278, -73.2574509 45.442776, -73.2574694 45.4428586, -73.2575396 45.4429687, -73.2575835 45.4430216, -73.2576515 45.443122, -73.2576886 45.4431709, -73.2578801 45.4434064, -73.257959 45.443531, -73.2581061 45.4436934, -73.2581498 45.443734, -73.2582317 45.4438242, -73.2582908 45.4439014, -73.2585447 45.4441762, -73.2586436 45.4442573, -73.2586947 45.4443133, -73.2589556 45.4445665, -73.2590081 45.4446235, -73.2591108 45.444702, -73.2593198 45.4448914, -73.259389 45.4449665, -73.2596361 45.4451632, -73.2597138 45.4452154, -73.2598763 45.4453479, -73.2600742 45.4455914, -73.260168 45.4456884, -73.2604196 45.4458332, -73.2605105 45.4459089, -73.2606834 45.446053 )"
			}
			
		};
		
		testData(points, expectedResults, Pourpoint.CType.NEAREST_INCATCHMENT.ccode);
		
	}
	
	//pourpoint c-code of -1 not supported at this time
//	@Test
//	public void test_FindDownstreamFlowpathsCcodeM1() throws ParseException {
//		
//		Coordinate[] points = new Coordinate[] {
//				new Coordinate(-73.25962985003476, 45.44781758003431),
//				new Coordinate(-73.28152158519661, 45.458804292183764),
//				new Coordinate(-73.36146746117232, 45.43173515441021),
//				new Coordinate(-73.35968693292561, 45.41697244599836)
//		};
//		
//		String[][] expectedResults = new String[][] {
//			{
//				"LINESTRING ( -73.2409249 45.4419336, -73.2428947 45.4425485, -73.2432902 45.4427055, -73.2445784 45.4431277, -73.2446284 45.4433258, -73.2445658 45.4433595, -73.2442694 45.443402, -73.2440564 45.4434318, -73.2439704 45.443449, -73.2439239 45.4434864, -73.2441712 45.4441308, -73.2442963 45.4443014, -73.2445686 45.4445698, -73.2445917 45.4446224, -73.2447291 45.4446927, -73.2449349 45.4447572, -73.2450152 45.4447994, -73.2453364 45.444856, -73.2454487 45.4448533, -73.2455942 45.4449335, -73.2456933 45.4449825, -73.2459257 45.4449819, -73.2461629 45.4449938, -73.2462568 45.4452962, -73.2464602 45.445757, -73.2466499 45.4461138, -73.2468168 45.4463436, -73.2471314 45.4462523, -73.2473354 45.4463348, -73.2477107 45.4464246, -73.2481145 45.446508, -73.2483533 45.446576, -73.2484965 45.4467074, -73.2486492 45.4468638, -73.2488254 45.4469604, -73.2489994 45.4469574, -73.2491265 45.4469565, -73.24921 45.4470728, -73.2492465 45.4472271, -73.2493491 45.447276, -73.2496893 45.4473009, -73.2497922 45.4472373, -73.2499974 45.4472504, -73.2500742 45.4472505, -73.2502402 45.4472599, -73.2503297 45.4472691, -73.2503937 45.4472512, -73.2503811 45.4472152, -73.2504196 45.4471703, -73.2504707 45.4471614, -73.2504962 45.4471885, -73.2505472 45.4472246, -73.2506112 45.4471977, -73.2506881 45.4471709, -73.2508031 45.4471622, -73.25088 45.4471533, -73.2511097 45.4472348, -73.2514798 45.4473616, -73.2519904 45.4475427, -73.2524755 45.4476967, -73.252884 45.4478236, -73.2532925 45.4479684, -73.2534839 45.4480499, -73.2536245 45.4480771, -73.2537138 45.4481043, -73.2538286 45.4481586, -73.2541989 45.4482853, -73.2545691 45.4484031, -73.2549137 45.4485118, -73.2551817 45.4486114, -73.255284 45.4486296, -73.2553352 45.4486117, -73.2557853 45.4479918, -73.2558365 45.4479739, -73.255926 45.4479651, -73.2561438 45.4478665, -73.2565921 45.4476695, -73.2566688 45.4476697, -73.2568347 45.447733, -73.2569754 45.4477334, -73.2571032 45.4477336, -73.2572054 45.4477519, -73.2573842 45.4477883, -73.2576142 45.4478157, -73.2577548 45.447825, -73.2578825 45.4478613, -73.2579079 45.4478883, -73.2579717 45.4479245, -73.2580611 45.4479426, -73.2581507 45.4479158, -73.258253 45.447907, -73.258317 45.4478892, -73.2584194 45.4478624, -73.2585345 45.4478537, -73.2585857 45.4478537, -73.2586752 45.4478449, -73.2587135 45.447836, -73.2587904 45.4478182, -73.2588414 45.4478273, -73.2589822 45.4477916, -73.2600966 45.447263 )"	
//			},
//			{
//				"LINESTRING (-73.2818869 45.4585994,-73.2815044 45.4589237)"	
//			},
//			{
//				"LINESTRING ( -73.3630852 45.4309296, -73.3626634 45.4309651, -73.3625229 45.4309469, -73.3622036 45.4308386, -73.3620249 45.4307664, -73.3617822 45.4306761, -73.3616929 45.4306309, -73.3616291 45.4306129, -73.3614886 45.4305678, -73.3612713 45.4305315, -73.3611691 45.4305494, -73.3609903 45.4305221, -73.3607475 45.4304858, -73.3604536 45.4304675, -73.3601085 45.430449 )"
//			},
//			{
//				"LINESTRING ( -73.3598754 45.4170579, -73.3598197 45.4171086, -73.3597597 45.4171719, -73.3597283 45.4172196, -73.3596972 45.4172673, -73.3596667 45.4173247, -73.3596435 45.4173726, -73.3596203 45.4174205, -73.3595972 45.4174684, -73.3595741 45.4175164, -73.3595551 45.4175673, -73.3595313 45.4176306, -73.3595209 45.4176932, -73.3595096 45.417761, -73.3594858 45.4178029, -73.3594935 45.4178413, -73.3594772 45.4179133, -73.3594608 45.4179853, -73.3594426 45.4180338, -73.3594629 45.4180807, -73.3594407 45.4181448, -73.3594592 45.4181916, -73.3594389 45.4182559, -73.3594651 45.41832, -73.3594675 45.4183566, -73.3594822 45.4184024 )"
//			}
//		};
//		
//		testData(points, expectedResults, Pourpoint.CType.NEAREST_FLOWPATH.ccode);
//	}
		
	
	@Test
	public void test_FindDownstreamFlowpathsCcode0() throws ParseException {
		
		Coordinate[] points = new Coordinate[] {
				new Coordinate(-73.35968693292561, 45.41697244599836),
				new Coordinate(-73.35863118925577, 45.41762587406392),
				new Coordinate(-73.35837694143224, 45.41778363809802),
				new Coordinate(-73.35836520691731, 45.41753460561445)
		};
		
		String[][] expectedResults = new String[][] {
			
			{
				"LINESTRING ( -73.3629605 45.4162388, -73.3628734 45.4162307, -73.3628094 45.416199, -73.3626908 45.4162049, -73.3625827 45.4161939, -73.3624235 45.4162112, -73.3622619 45.416227, -73.362096 45.4162419, -73.3619146 45.4162608, -73.3618402 45.4162908, -73.3617817 45.416293, -73.361679 45.4163296, -73.361544 45.416372, -73.3613757 45.4164252, -73.3612523 45.4164908, -73.3611974 45.4164869, -73.3610209 45.4165512, -73.3608573 45.4166027, -73.3606913 45.4166564, -73.3605929 45.4167105, -73.3605264 45.4167117, -73.3604638 45.4167574, -73.3603568 45.4167918, -73.3602544 45.4168424, -73.3601648 45.4168873, -73.3600798 45.4169367, -73.3599948 45.4169861, -73.3599425 45.4170059, -73.3598754 45.4170579 )"
			},
			{
				"LINESTRING ( -73.3555437 45.4163696, -73.3559901 45.4166851, -73.3577627 45.4180282, -73.3580051 45.4181904, -73.3582868 45.4179299, -73.3585431 45.4176152 )",
				"LINESTRING ( -73.3575775 45.4154633, -73.357577 45.4156793, -73.3575382 45.4158593, -73.3575244 45.4162462, -73.3575495 45.4164172, -73.357613 45.4165882, -73.3578929 45.4170565, -73.3580075 45.4171916, -73.3583902 45.417453, -73.3585431 45.4176152 )"
			},
			{
				"LINESTRING ( -73.3555437 45.4163696, -73.3559901 45.4166851, -73.3577627 45.4180282, -73.3580051 45.4181904, -73.3582868 45.4179299, -73.3585431 45.4176152 )",
				"LINESTRING ( -73.3575775 45.4154633, -73.357577 45.4156793, -73.3575382 45.4158593, -73.3575244 45.4162462, -73.3575495 45.4164172, -73.357613 45.4165882, -73.3578929 45.4170565, -73.3580075 45.4171916, -73.3583902 45.417453, -73.3585431 45.4176152 )"
			},
			{
				"LINESTRING ( -73.3555437 45.4163696, -73.3559901 45.4166851, -73.3577627 45.4180282, -73.3580051 45.4181904, -73.3582868 45.4179299, -73.3585431 45.4176152 )",
				"LINESTRING ( -73.3575775 45.4154633, -73.357577 45.4156793, -73.3575382 45.4158593, -73.3575244 45.4162462, -73.3575495 45.4164172, -73.357613 45.4165882, -73.3578929 45.4170565, -73.3580075 45.4171916, -73.3583902 45.417453, -73.3585431 45.4176152 )"
			},
		};
		
		testData(points, expectedResults, Pourpoint.CType.NEAREST_NEXUS_ALL.ccode);
	}
	
	@Test
	public void test_FindDownstreamFlowpathsCcodeP1() throws ParseException {
		
		Coordinate[] points = new Coordinate[] {
				new Coordinate( -73.35753094105937, 45.41535411226546),
				new Coordinate(-73.23713731644023, 45.386258121892475),
				new Coordinate(-73.24676505616975, 45.50886001754486),
			    new Coordinate(-73.46806961261278, 45.096560760069536)
		};
		
		String[][] expectedResults = new String[][] {
			
			{
				"LINESTRING ( -73.3467758 45.4079626, -73.3468013 45.4079896, -73.346916 45.4080707, -73.3469799 45.4080978, -73.3472477 45.4082692, -73.3473242 45.4083322, -73.3480892 45.4088552, -73.3481658 45.4089093, -73.3482168 45.4089543, -73.3482805 45.4089994, -73.3483188 45.4090264, -73.3483443 45.4090445, -73.3484973 45.4091437, -73.3485611 45.4091887, -73.3486631 45.4092608, -73.3488289 45.4093601, -73.3489437 45.4094232, -73.3490458 45.4094953, -73.3492497 45.4096306, -73.3493135 45.4096847, -73.3493645 45.4097297, -73.3494536 45.4098199, -73.3495301 45.4098919, -73.3496704 45.4099911, -73.3497596 45.4100542, -73.3498745 45.4101263, -73.350053 45.4102525, -73.3501039 45.4102976, -73.3501677 45.4103516, -73.3502952 45.4104418, -73.350359 45.4104869, -73.3504228 45.410514, -73.3505376 45.4105862, -73.3506139 45.4106672, -73.350665 45.4107033, -73.3507288 45.4107483, -73.3509074 45.4108655, -73.3509966 45.4109196, -73.3510731 45.4109828, -73.3512389 45.411091, -73.3513282 45.4111451, -73.3514302 45.4112082, -73.3515195 45.4112713, -73.3516086 45.4113614, -73.3516852 45.4114155, -73.3517362 45.4114695, -73.3519148 45.4115777, -73.3520551 45.4116679, -73.3521315 45.411713, -73.3522209 45.4117851, -73.3523484 45.4118663, -73.3524122 45.4119204, -73.3524886 45.4119654, -73.3526161 45.4120646, -73.3526671 45.4121006, -73.352731 45.4121457, -73.3528457 45.4122268, -73.3529223 45.4122719, -73.353037 45.4123621, -73.3531008 45.4124161, -73.3532028 45.4124703, -73.3532665 45.4125154, -73.3534323 45.4126326, -73.3536236 45.4127588, -73.3537639 45.4128579, -73.3538532 45.412921, -73.3539936 45.4129932, -73.35407 45.4130473, -73.3541338 45.4130923, -73.3541976 45.4131465, -73.3543123 45.4132456, -73.3543633 45.4132907, -73.3544144 45.4133357, -73.354478 45.4133808, -73.3545546 45.4134258, -73.3547077 45.4135341, -73.3547842 45.4135791, -73.3548607 45.4136332, -73.3550009 45.4137503, -73.3550648 45.4137955, -73.3551284 45.4138405, -73.3552051 45.4138856, -73.3552815 45.4139307, -73.3554346 45.4140209, -73.3555112 45.4140749, -73.3555876 45.414129, -73.3556514 45.4141832, -73.3557789 45.4142733, -73.3558428 45.4143183, -73.3558938 45.4143544, -73.3559447 45.4143904, -73.3560596 45.4144806, -73.3561105 45.4145166, -73.3561743 45.4145618, -73.3562254 45.4146068, -73.3563018 45.4146699, -73.3563911 45.414742, -73.3564548 45.414787, -73.3566207 45.4148863, -73.3567227 45.4149494, -73.356812 45.4150035, -73.356863 45.4150665, -73.3570162 45.4151387, -73.3570671 45.4151838, -73.3571181 45.4152198, -73.3571947 45.4152739, -73.3573094 45.415364, -73.3573859 45.4154091, -73.3574625 45.4154542, -73.3575775 45.4154633 )"
			},
			{
				"LINESTRING ( -73.2361135 45.3925079, -73.2362031 45.3924631, -73.2366129 45.3922211, -73.2373686 45.3917459, -73.2376759 45.3915756, -73.2378554 45.391432, -73.2379839 45.3912614, -73.2381254 45.3910278, -73.2382284 45.390866, -73.2383187 45.3906772, -73.2384345 45.3904795, -73.2385631 45.3902909, -73.2387173 45.3900753, -73.2387563 45.3899584, -73.2388078 45.3898235, -73.2388466 45.3897156, -73.2390688 45.3886273, -73.2383446 45.3878248, -73.2374936 45.3868329, -73.2371 45.3863371 )"
			},
			{
				"LINESTRING ( -73.2507253 45.5066442, -73.2507022 45.5066646, -73.250587 45.5067738, -73.2505514 45.5068048, -73.2504757 45.5068641, -73.2504138 45.5069029, -73.2503548 45.506942, -73.2501636 45.5070762, -73.2500968 45.5071305, -73.2499956 45.5071975, -73.2498723 45.5072966, -73.2497904 45.5073503, -73.2496548 45.5074241, -73.2496151 45.5074489, -73.2494825 45.5075427, -73.2493679 45.5076346, -73.2492702 45.5077184, -73.2492378 45.5077479, -73.2490917 45.5078558, -73.2490331 45.507897, -73.2489541 45.5079554, -73.2488654 45.5080144, -73.2488071 45.5080554, -73.2485763 45.5082269, -73.2485109 45.5082598, -73.2484181 45.5083085, -73.2482713 45.5084127, -73.2482205 45.5084496, -73.248162 45.5084869, -73.2480085 45.5085795, -73.2479113 45.508654, -73.247857 45.5086971, -73.2478011 45.5087391, -73.2476936 45.5088171, -73.2476478 45.5088617, -73.2475871 45.508919, -73.247526 45.5089734 )"
			},
			{
				"LINESTRING ( -73.469757 45.0982403, -73.4697499 45.0976454, -73.4695442 45.0975688, -73.4692061 45.0974987, -73.4682618 45.0973156, -73.4678536 45.0970856 )"
			}

		};
		
		testData(points, expectedResults, 1);
		
		
		points = new Coordinate[] {
				new Coordinate( -73.35753094105937, 45.41535411226546),
				new Coordinate(-73.23713731644023, 45.386258121892475),			    
				new Coordinate(-73.46806961261278, 45.096560760069536)
		};
		
		expectedResults = new String[][] {
			
			{
				"LINESTRING ( -73.3585268 45.4138898, -73.3585779 45.4139259, -73.3585905 45.4139708, -73.3585776 45.4140248, -73.3585776 45.4140698, -73.3585646 45.4141238, -73.3585006 45.4142047, -73.3583854 45.4142675, -73.358155 45.4144652, -73.3580651 45.4146091, -73.3579109 45.4149599, -73.3576927 45.4153285, -73.3575775 45.4154633 )"
			},
			{
				"LINESTRING ( -73.2339475 45.3886875, -73.2341015 45.3885258, -73.2342041 45.3884271, -73.2342555 45.3883552, -73.2345121 45.3881128, -73.2350377 45.3876731, -73.2351916 45.3875475, -73.2352815 45.3874488, -73.2353583 45.3874039, -73.2353839 45.387386, -73.2355631 45.3873054, -73.235947 45.3871083, -73.236395 45.3869114, -73.2367661 45.3867413, -73.23683 45.3867144, -73.2368814 45.3866695, -73.2368945 45.3865796, -73.2369461 45.3864627, -73.2369848 45.3863908, -73.2371 45.3863371 )"
			},
			{
				"LINESTRING ( -73.4667708 45.1041613, -73.4670632 45.1039814, -73.4673174 45.1037925, -73.4675844 45.1035676, -73.4678006 45.1033157, -73.4680168 45.1030187, -73.4681187 45.1027309, -73.4682077 45.1024519, -73.4682206 45.1021909, -73.4681321 45.101264, -73.4681195 45.101102, -73.4680056 45.1004541, -73.4679042 45.0998691, -73.4678157 45.0991041, -73.4678292 45.0976372, -73.4678536 45.0970856 )"
			}

		};
		
		testData(points, expectedResults, 2);
		
		
		points = new Coordinate[] {
				new Coordinate(-73.23713731644023, 45.386258121892475)
		};
		
		expectedResults = new String[][] {
			
			{
				"LINESTRING ( -73.2343192 45.3829202, -73.2354133 45.3838226, -73.235347 45.3843444, -73.2353592 45.3844613, -73.2354099 45.3845515, -73.2362612 45.3854623, -73.2367948 45.3860485, -73.2371 45.3863371 )"
			},

		};
		
		testData(points, expectedResults, 3);
	}
	
	
	private void testData(Coordinate[] points, String[][] expectedResults, int ccode) throws ParseException {
		for (int i = 0; i < points.length; i ++) {
			Coordinate c = points[i];
			String[] thisResult = expectedResults[i];
			
			Point pnt = GeotoolsGeometryReprojector.reproject(BasicTestSuite.GF.createPoint(c),  BasicTestSuite.TEST_CRS, ChyfDatastore.BASE_CRS);
			
			Pourpoint pp = new Pourpoint(pnt, ccode, String.valueOf(i));
			pp.findDownstreamFlowpaths(BasicTestSuite.DATASTORE.getHyGraph());
			List<EFlowpath> actualResults = pp.getDownstreamFlowpaths();
			
//			System.out.println("TEST: POINT(" + c.x + " " + c.y + ")");
//			for (EFlowpath root: actualResults) {
//				Geometry g = GeotoolsGeometryReprojector.reproject(root.getLineString(), BasicTestSuite.TEST_DATA_SRID);
//				System.out.println(g.toText());
//			}
			Assert.assertEquals("Pourpoint downstream flowpath test return incorrect number of results at (" + c.x + " " + c.y + ")", thisResult.length,  actualResults.size());
			WKTReader reader = new WKTReader(BasicTestSuite.GF);
			for (String ls : thisResult) {
				Geometry g = reader.read(ls);
				Geometry projection = GeotoolsGeometryReprojector.reproject(g,  BasicTestSuite.TEST_CRS, ChyfDatastore.BASE_CRS);

				EFlowpath found = null;
				for (EFlowpath p : actualResults) {
					if (p.getLineString().equalsExact(projection, 0.00001)) {
						found = p;
						break;
					}
				}
				Assert.assertNotNull("Pourpoint downstream flowpath test return incorrect result at (" + c.x + " " + c.y + ")", found);
				if (found != null) actualResults.remove(found);
			}
		}
	}
}
