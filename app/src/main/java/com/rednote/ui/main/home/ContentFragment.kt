package com.rednote.ui.main.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.rednote.R
import com.rednote.data.model.main.FeedItem
import com.rednote.databinding.FragmentContentBinding
import com.rednote.ui.base.BaseFragment
import com.rednote.ui.base.EmptyViewModel

class ContentFragment : BaseFragment<FragmentContentBinding, EmptyViewModel>() {

    override val viewModel: EmptyViewModel by viewModels()

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentContentBinding {
        return FragmentContentBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        // 设置瀑布流布局
        val layoutManager = StaggeredGridLayoutManager(2, RecyclerView.VERTICAL).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }
        binding.rvFeed.layoutManager = layoutManager
        binding.rvFeed.setHasFixedSize(true)
    }

    override fun initData() {
        // 加载模拟数据
        binding.rvFeed.adapter = FeedAdapter(generateMockFeed())
    }

    private fun generateMockFeed(): List<FeedItem> = listOf(
        FeedItem(
            title = "发现一个奇怪的现象",
            description = "6个月的宝宝在家每天嘎嘎笑，一出门就变得面无表情，逗她也不笑了,为什么？",
            author = "禾禾小宝贝在家呦",
            likeCount = 123,
            imageRes = R.drawable.ic_launcher_background
        ),
        FeedItem(
            title = "突然发现小狗竟然有嘴唇子！",
            description = "养了多年的狗狗第一次认真看它，竟然发现它也有嘴唇，这算大发现吗哈哈哈。",
            author = "我又又又馋了",
            likeCount = 670,
            imageRes = R.drawable.ic_launcher_background
        ),
        FeedItem(
            title = "广州常去的一家咖啡店",
            description = "安静又有设计感，适合坐上一下午发呆或办公。",
            author = "一只开心",
            likeCount = 20,
            imageRes = R.drawable.ic_launcher_background
        ),
        FeedItem(
            title = "坐地铁醒来抬头叮一跳",
            description = "抬头看到对面大爷在练臂力，一时不知道该不该假装没看到。",
            author = "认真生活",
            likeCount = 88,
            imageRes = R.drawable.ic_launcher_background
        ),
        FeedItem(
            title = "突然想做饭",
            description = "第一次尝试做广式早茶，小伙伴们都说味道还行。",
            author = "广州吃吃吃",
            likeCount = 56,
            imageRes = R.drawable.ic_launcher_background
        ),
        FeedItem(
            title = "一束光的记录",
            description = "雨后夕阳照进客厅的那一刻真的太治愈了，必须记录下来。",
            author = "随手记",
            likeCount = 42,
            imageRes = R.drawable.ic_launcher_background
        ),
        FeedItem(
            title = "我家猫的新玩具",
            description = "给猫买了个假老鼠，她竟然怕得绕着走，猫咪的心思太难猜。",
            author = "猫大人",
            likeCount = 304,
            imageRes = R.drawable.ic_launcher_background
        ),
        FeedItem(
            title = "下班后的小确幸",
            description = "和同事去河边骑行，吹着晚风超舒服，瞬间把工作压力忘了。",
            author = "城南打工人",
            likeCount = 129,
            imageRes = R.drawable.ic_launcher_background
        ),
        FeedItem(
            title = "发现一家隐藏在巷子的花店",
            description = "老板娘只做预约花束，每束花都配色清新，很适合送礼。",
            author = "小城日记",
            likeCount = 214,
            imageRes = R.drawable.ic_launcher_background
        ),
        FeedItem(
            title = "夜跑的意义",
            description = "坚持夜跑 30 天，身体状态真的好了很多，也更爱自己的城市了。",
            author = "热爱运动",
            likeCount = 98,
            imageRes = R.drawable.ic_launcher_background
        )
    )
}