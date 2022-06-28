package iptv.util;

import java.text.SimpleDateFormat;

public interface BizConstant {

	interface Code{


		String Code_YES="Y";
		String Code_NO="N";

		//结果
		//成功
		String Result_Code_Success="SUCCESS";
		//失败
		String Result_Code_Fail="FAIL";

		//成功
		String Result_Code_Success_0000="0000";

		//成功
		String Result_Code_Success_Num_0="0";
		//失败
		String Result_Code_Fail_Num_1="1";
		//失败
		String Result_Code_Fail_Num="-1";
		//非法请求
		String Result_Code_Illegal="998";
		//系统错误
		String Result_Code_Systemrror="999";

		//停止订单发货成功标志
		String Result_Order_Success_000="000";

		//停止订单发货失败标志
		String Result_Order_Fail_999="999";

		//缺少参数
		String Missing_Parameter="4000";
		//非法参数
		String llegal_Parameter="4001";



		//验证签名出错
		String Signs_Error="4004";

		//新增
		String EditBiliProd_Optype_Add="1";
		//修改
		String EditBiliProd_Optype_Update="2";

		//找不到数据
		String Find_Data_Null="7777";

		//url请求超时或出错
		String Requst_Url_Error="7778";

		//url请求返回为空
		String Requst_Url_Return_Null="7779";





		interface Proudct{
			//产品已经存在
			String Product_IsExist_Error="4002";

			//产品不存在
			String Product_IsNotExist_Error="4003";

			//产品类型
			//包月
			String Type_svod="svod";
			//单点
			String Type_tvod="tvod";
			//充值
			String Type_fvod="fvod";
			//全部类型
			String Type_allvod="allvod";


			//是否自动续费
			//是
			String Contract_Yes="1";
			//否
			String Contract_No="0";
		}


		interface Order{
			//订单支付状态
			//未支付
			int Order_Pay_Status_Notpay=0;
			//已支付
			int Order_Pay_Status_Paied=1;

			//订单支付类型
			//支付宝
			String Order_Pay_Type_Alipay="1";
			//微信
			String Order_Pay_type_WeiXin="2";

			//订单支付方式
			//扫码支付
			String PayWay_Scan="1";
			//app支付
			String PayWay_App="2";


			//支付完成通知B站订单的状态
			String Bili_Trade_State_Success="SUCCESS";//支付成功
			String Bili_Trade_State_Refund="REFUND";//转入退款
			String Bili_Trade_State_Notpay="NOTPAT";//未支付
			String Bili_Trade_State_Closed="CLOSED";//已关闭
			String Bili_Trade_State_Accept="ACCEPT";//已接受，等待扣款
			String Bili_Trade_State_PayFail="PAY_FAIL";//支付失败（其他原因，如银行返回失败）


			int Notify_Bili_Status_Success=0;
			int Notify_Bili_Status_Fail=1;

			//调用支付网关支付方式
			//微信
			String Order_To_SnmBoss_PayType_Wx="wx";
			//支付宝
			String Order_To_SnmBoss_PayType_Zfb="zfb";

			//调用支付网关支付类型
			//扫码支付
			String Order_To_SnmBoss_TradeType_NATIVE="NATIVE";
			//app支付
			String Order_To_SnmBoss_TradeType_APP="APP";



			//支付回调重发错误记录状态
			//重发
			String Bili_Order_Callback_Reply_Status_ReSend="0";
			//超过错误次数 redis队列里面的数据
			String Bili_Order_Callback_Reply_Status_Redis_MaxCount="1";
			//超过错误次数 数据库里面的数据
			String Bili_Order_Callback_Reply_Status_Db_MaxCount="2";
			//商户订单号traceno在我们库里找不到对应的订单记录
			String Bili_Order_Callback_Reply_Status_OrderNotExist="3";
			//商户订单号traceno已经通知成功
			String Bili_Order_Callback_Reply_Status_OrderNotified="4";

			//运营方权益计算方式-自然月
			String Compute_Mode_Natural_Month = "1";
			//运营方权益计算方式-固定天数
			String Compute_Mode_Days = "2";

			//第三方视频平台-腾讯编码
			String Cooperator_Code_Tencent = "tencent";
			//第三方视频平台-优酷编码
			String Cooperator_Code_Youku = "youku";
			//第三方视频平台-爱奇艺编码
			String Cooperator_Code_Aiqiyi = "aiqiyi";
			//优酷线上支付订单(线上应用内购买)
			String Order_Type_Youku_Online = "1";
			//优酷线上支付订单(线上应用内购买)
			String Order_Type_Youku_Offline = "2";
			//连续包取消续订
			String Order_Type_Youku_CancelRenew = "3";
			//全额退款(立即终止权益,不分产品包,不计财务)
			String Order_Type_Youku_RefundRenew = "4";
			//续费(运营商侧发起时才使用)
			String Order_Type_Youku_Renew = "5";
			//非连续包退订(按未使用天数退款)
			String Order_Type_Youku_RefundOrder = "6";
		}

		interface ZnyxOrder {
			//订单支付状态
			//未支付
			String Order_Pay_Status_Notpay="0";
			//已支付
			String Order_Pay_Status_Paied="1";
		}

		interface User{

			//用户类型
			//QQ用户
			String Type_qq="qq";
			//微信用户
			String Type_wx="wx";
			//手机用户
			String Type_phone="ph";
			//邮箱用户
			String Type_mail="yx";
			//其他用户
			String Type_vu="vu";


		}


	}

	interface Tencent{

		//订单处理状态
		//下单失败
		String Order_Status_Create_Fail="0";
		//下单成功，发货失败
		String Order_Status_Pay_Fail="1";
		//下单成功，发货成功
		String Order_Status_Pay_Success="2";
		//发货中
		String Order_Status_Pay_Shipping="3";

		//子订单处理状态
		//待发货
		String OrderDet_Status_Create_Prepare="-1";
		//下单失败
		String OrderDet_Status_Create_Fail="0";
		//下单成功，发货失败
		String OrderDet_Status_Pay_Fail="1";
		//下单成功，发货成功
		String OrderDet_Status_Pay_Success="2";

		//腾讯返回订单状态
		//待发货
		int Tx_Order_Status_TobeShipped=0;
		//已发货
		int Tx_Order_Status_Shipped=1;
		//非法的access_token
		String AccessToken_Invalid_Code="231";

		//订单类型
		//非出账,非补订购订单
		String Order_Type_Bill="1";
		//出账订单
		String Order_Type_Payment="2";
		//后台补订购订单
		String Order_Type_Replenishment="3";


		interface Resend{

			//初始状态
			String Mobile_Order_Status_Max_Init="0";
			//超过最大重试次数
			String Mobile_Order_Status_Max_Nums="1";
			//初始状态
			String Mobile_Order_Status_Success="2";

		}

		interface VipInfo{
			//会员包id（平台相关，需要事先约定，如3：基础包；
			//  4：hbo；35：nba
			int vip_bid_basic=3;
			int vip_bid_hbo=4;
			int vip_bid_nba=35;
			int vip_bid_cinema=104;

		}

	}

	interface Youku{
		//发货失败
		String Order_Status_Pay_Fail="0";
		//发货成功
		String Order_Status_Pay_Success="2";
		//退费成功
		String Order_Status_Pay_Refund="4";

		//移动
		String Youku_Source_Yidong = "snm_yidong";
		//电信
		String Youku_Source_Dianxin = "snm_dianxin";
	}

	interface MobileUserProduct{

		//在用
		String Pstatus_Useid="2";
		//停用
		String Pstatus_Stop="3";

		//自动续费 0
		String IsAutoPay_Yes="0";

		//非自动续费 1
		String IsAutoPay_No="1";


	}

	interface IncidentPush{
		//告警严重级别：normal(普通)，warn(一般)，alarm(严重)，error(致命)
		String Lvlcode_Normal="normal";
		String Lvlcode_Warm="warn";
		String Lvlcode_Alarm="alarm";
		String Lvlcode_Error="error";

		//事件级别： warn(预警),alarm(告警)
		String Incidentcategory_Warm="warn";
		String Incidentcategory_Alarm="alarm";
		SimpleDateFormat httpUtilsdateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");

	}

	interface BillReconciliation{
		//1 ：tx库有，平台没有  ；2：tx库没有，平台有 ； 3：tx库与平台价格不一致数据
		String ProblemType_Local="1";
		String ProblemType_Plat="2";
		String ProblemType_Price="3";

		//未处理
		String ProblemStatus_Untreated="1";
		//已处理
		String ProblemStatus_treated="2";

		//支付
		String Type_Pay="0";
		//退款
		String Type_Refund="1";
	}


	interface RedisKey{
		//redis键值对定义

		//http请求异常次数
		String AlarmHttpErrorCount="AlarmHttpErrorCount";

		//请求腾讯下单成功之后,返回信息缓存在redis的键值
		String TxCreateOrder="TxCreateOrder_";


	}

	interface Aiqiyi{
		//成功代码
		String SuccessCode = "A00000";
		//成功消息
		String SuccessMsg = "成功";

		//参数错误代码
		String ParameterErrorCode = "Q00301";
		//参数错误消息
		String ParameterErrorMsg = "参数错误";

		//签名错误代码
		String SignatureErrorCode = "Q00307";
		//签名错误消息
		String SignatureErrorMsg = "签名错误";

		//系统错误代码
		String SystemErrorCode = "Q00332";
		//系统错误消息
		String SystemErrorMsg = "系统错误";

		//非法的IP代码
		String IllegalIPCode = "Q00347";
		//非法的IP消息
		String IllegalIPMsg = "非法的IP";

		//查询点播劵信息失败代码
		String QueryOnDemandSecuritiesFailureCode = "Q00353";
		//查询点播劵信息失败消息
		String QueryOnDemandSecuritiesFailureMsg = "查询点播劵信息失败";

		//账号转换失败代码
		String AccountConversionFailureCode = "Q00304";
		//账号转换失败消息
		String AccountConversionFailureMsg = "账号转换失败";

		//没有会员权益代码
		String NoRightsCode = "Q00352";
		//没有会员权益消息
		String NoRightsMsg = "没有会员权益";

		//查询会员信息失败代码
		String QueryMemberInformationFailureCode = "Q00413";
		//查询会员信息失败消息
		String QueryMemberInformationFailureMsg = "查询会员信息失败";


		//原订单不存在或状态不是已完成代码
		String OrderDoesNotExistHasBeenFinishCode = "Q00409";
		//原订单不存在或状态不是已完成消息
		String OrderDoesNotExistHasBeenFinishMsg = "原订单不存在或状态不是已完成";

		//原订单上没有可退的会员权益代码
		String NoRefundRightsCode = "Q00426";
		//原订单上没有可退的会员权益消息
		String NoRefundRightsMsg = "原订单上没有可退的会员权益";

		//不支持合作方接口退单代码
		String DoNotSupportReturnSingleCode = "Q00429";
		//不支持合作方接口退单消息
		String DoNotSupportReturnSingleMsg = "不支持合作方接口退单";

		//用户已参加优惠活动代码
		String HaveToDiscountCode = "Q00601";
		//用户已参加优惠活动消息
		String HaveToDiscountMsg = "用户已参加优惠活动";

		//无效的续费用户代码
		String InvalidUserCode = "Q00602";
		//无效的续费用户消息
		String InvalidUserMsg = "无效的续费用户";

		//请求已达上限代码
		String RequestCapCode = "Q00603";
		//请求已达上限消息
		String RequestCapMsg = "请求已达上限";

		//非法的续费日期代码
		String IllegalDateCode = "Q00604";
		//非法的续费日期消息
		String IllegalDateMsg = "非法的续费日期";

		//尚未配置优惠策略代码
		String NotConfiguredPreferentialCode = "Q00605";
		//尚未配置优惠策略消息
		String NotConfiguredPreferentialMsg = "尚未配置优惠策略";

		//重复的续费订单代码
		String RepeatOrdersCode = "Q00606";
		//重复的续费订单消息
		String RepeatOrdersMsg = "重复的续费订单";

		//合作方不可用代码
		String PartnerIsNotAvailableCode = "Q00713";
		//合作方不可用消息
		String PartnerIsNotAvailableMsg = "合作方不可用";

		//发货成功
		String Order_Status_Pay_Success="2";
	}

	interface ZNYX{

		/**
		 * 成功状态值
		 */
		String Result_Code_Success_Num_0 = "0";

		/**
		 * 初始化状态状态值
		 */
		String initStatus = "-1";

		/**
		 * 通知成功状态值
		 */
		String Notify_Success_Status = "9";

	}
}
