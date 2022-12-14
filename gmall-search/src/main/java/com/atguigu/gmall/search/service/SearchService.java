package com.atguigu.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchParamVo;
import com.atguigu.gmall.search.pojo.SearchResponseAttrVo;
import com.atguigu.gmall.search.pojo.SearchResponseVo;
import io.jsonwebtoken.lang.Collections;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public SearchResponseVo search(SearchParamVo searchParam)  {

        try {
            SearchResponse response = this.restHighLevelClient.search(new SearchRequest(new String[]{"goods"}, this.buildDSL(searchParam)), RequestOptions.DEFAULT);
            SearchResponseVo responseVo = parseResult(response);
            // ??????????????????
            responseVo.setPageNum(searchParam.getPageNum());
            responseVo.setPageSize(searchParam.getPageSize());
            return responseVo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SearchResponseVo parseResult(SearchResponse response){
        SearchResponseVo responseVo = new SearchResponseVo();

        // ?????????????????????????????????????????????
        SearchHits hits = response.getHits();
        // ????????????
        responseVo.setTotal(hits.getTotalHits().value);
        // ???????????????
        SearchHit[] hitsHits = hits.getHits();
        if (hitsHits != null && hitsHits.length > 0){
            responseVo.setGoodsList(Arrays.stream(hitsHits).map(hitsHit -> {
                String json = hitsHit.getSourceAsString();
                Goods goods = JSON.parseObject(json, Goods.class);
                // ??????????????????
                Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
                HighlightField title = highlightFields.get("title");
                Text fragment = title.getFragments()[0];
                goods.setTitle(fragment.string());
                return goods;
            }).collect(Collectors.toList()));
        }


        // ???????????????????????????????????????
        Aggregations aggregations = response.getAggregations();
        ParsedLongTerms brandIdAgg = aggregations.get("brandIdAgg");
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        if (!Collections.isEmpty(buckets)){
            responseVo.setBrands(buckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                brandEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                Aggregations aggs = ((Terms.Bucket) bucket).getAggregations();
                ParsedStringTerms brandNameAgg = aggs.get("brandNameAgg");
                List<? extends Terms.Bucket> brandNameAggBuckets = brandNameAgg.getBuckets();
                if (!Collections.isEmpty(brandNameAggBuckets)){
                    brandEntity.setName(brandNameAggBuckets.get(0).getKeyAsString());
                }
                ParsedStringTerms logoAgg = aggs.get("logoAgg");
                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                if (!Collections.isEmpty(logoAggBuckets)){
                    brandEntity.setLogo(logoAggBuckets.get(0).getKeyAsString());
                }
                return brandEntity;
            }).collect(Collectors.toList()));
        }
        // ????????????id???????????????
        ParsedLongTerms categoryIdAgg = aggregations.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if (!Collections.isEmpty(categoryIdAggBuckets)){
            responseVo.setCategories(categoryIdAggBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                categoryEntity.setId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                ParsedStringTerms categoryNameAgg = ((Terms.Bucket) bucket).getAggregations().get("categoryNameAgg");
                List<? extends Terms.Bucket> categoryNameAggBuckets = categoryNameAgg.getBuckets();
                if (!Collections.isEmpty(categoryNameAggBuckets)){
                    categoryEntity.setName(categoryNameAggBuckets.get(0).getKeyAsString());
                }
                return categoryEntity;
            }).collect(Collectors.toList()));
        }

        // ??????????????????
        ParsedNested attrAgg = aggregations.get("attrAgg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrIdAggBuckets = attrIdAgg.getBuckets();
        if (!Collections.isEmpty(attrIdAggBuckets)){
            List<SearchResponseAttrVo> filters = attrIdAggBuckets.stream().map(bucket -> {
                SearchResponseAttrVo responseAttrVo = new SearchResponseAttrVo();
                responseAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                Aggregations aggrs = ((Terms.Bucket) bucket).getAggregations();
                ParsedStringTerms attrNameAgg = aggrs.get("attrNameAgg");
                List<? extends Terms.Bucket> attrNameAggBuckets = attrNameAgg.getBuckets();
                if (!Collections.isEmpty(attrNameAggBuckets)) {
                    responseAttrVo.setAttrName(attrNameAggBuckets.get(0).getKeyAsString());
                }
                ParsedStringTerms attrValueAgg = aggrs.get("attrValueAgg");
                List<? extends Terms.Bucket> attrValueAggBuckets = attrValueAgg.getBuckets();
                if (!Collections.isEmpty(attrValueAggBuckets)) {
                    responseAttrVo.setAttrValues(
                            attrValueAggBuckets.stream().map(Terms.Bucket::getKeyAsString)
                                    .collect(Collectors.toList()));
                }
                return responseAttrVo;
            }).collect(Collectors.toList());
            responseVo.setFilters(filters);
        }
        return responseVo;
    }

    private SearchSourceBuilder buildDSL(SearchParamVo searchParam) {

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
// ??????????????????????????????
        String keyword = searchParam.getKeyword();
        if (StringUtils.isBlank(keyword)){
            // TODO ?????????
            throw new RuntimeException("????????????????????????");
        }

        // 1.???????????????????????????
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);
         // 1.1 ??????????????????
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));

        // 1.2 ??????????????????
        // 1.2.1 ?????????????????????
        List<Long> brandId = searchParam.getBrandId();
        if (!Collections.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandId));
        }
        // 1.2.2 ??????????????????
        List<Long> categoryId = searchParam.getCategoryId();
        if (!Collections.isEmpty(categoryId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",categoryId));
        }
        //1.2.3 ??????????????????
        Double priceFrom = searchParam.getPriceFrom();
        Double priceTo = searchParam.getPriceTo();
        // ?????????????????????????????????????????????????????????????????????
        if (priceFrom != null || priceTo != null){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            boolQueryBuilder.filter(rangeQuery);
            if (priceFrom != null){
                rangeQuery.gte(priceFrom);
            }
            if (priceTo != null){
                rangeQuery.gte(priceTo);
            }
        }
        // 1.2.4 ????????????
        Boolean store = searchParam.getStore();
        // ??????????????????????????????????????????????????????????????????????????????
        if (store != null){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("store",store));
        }
        // 1.2.5 ??????????????????
        List<String> props = searchParam.getProps();
        if (!Collections.isEmpty(props)){
            props.forEach(prop -> {
                String[] attrs = StringUtils.split(prop, ":");
                if (attrs != null && attrs.length == 2 && NumberUtils.isCreatable(attrs[0])){
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId",attrs[0]));
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrValue",StringUtils.split(attrs[1],"-")));
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs",boolQuery, ScoreMode.None));
                }
            });
        }
        // 2.??????????????????
        Integer sort = searchParam.getSort();
        if (store != null){
            switch (sort){
                case 1:sourceBuilder.sort("price", SortOrder.DESC);break;
                case 2:sourceBuilder.sort("price", SortOrder.ASC);break;
                case 3:sourceBuilder.sort("sales", SortOrder.DESC);break;
                case 4:sourceBuilder.sort("createTime", SortOrder.DESC);break;
                default:
                    sourceBuilder.sort("score", SortOrder.DESC);
            }
        }
        // 3?????????????????????
        Integer pageNum = searchParam.getPageNum();
        Integer pageSize = searchParam.getPageSize();
        sourceBuilder.from((pageNum -1) * pageSize);
        sourceBuilder.size(pageSize);
        // 4.??????
        sourceBuilder.highlighter(new HighlightBuilder()
                .field("title")
                .preTags("<font style='color:red'>")
                .postTags("</font>"));
        // 5. ????????????
        // 5.1 ????????????
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));
        // 5.2 ????????????
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                 .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
        // 5.3 ??????????????????
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));

        sourceBuilder.fetchSource(new String[]{"skuId", "title", "subtitle", "defaultImage", "price"},null);
        System.out.println(sourceBuilder);
        return sourceBuilder;
    }

}
