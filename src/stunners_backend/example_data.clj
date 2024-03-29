(ns stunners-backend.example-data
  (:require [stunners-backend.datomic :refer [conn]]
            [datomic.api :as d]))

(def txs [[:db/add "dave" :user/name "Dave Martin"]
          [:db/add "dave" :location/address "3 Gassiot Road, Tooting, London, SW178LB"]
          [:db/add "dave" :location/lat 51.429813]
          [:db/add "dave" :location/lng -0.163293]
          [:db/add "dave" :location/outcode "SW17"]
          [:db/add "dave" :location/postcode "SW17 8LB"]
          [:db/add "dave" :user/avatar "https://scontent-lhr3-1.xx.fbcdn.net/v/t1.0-9/10996073_10207487464452820_6409447732558825236_n.jpg?oh=fce586ec483954802fb67f3ad47aa505&oe=5ABDE97A"]
          [:db/add "dave" :stylist/headline "The best stylist ever. Also super handsome."]
          [:db/add "dave" :stylist/images "https://thumb7.shutterstock.com/display_pic_with_logo/3014402/405719401/stock-photo-beautiful-woman-getting-haircut-by-hairdresser-in-the-beauty-salon-405719401.jpg"]
          [:db/add "dave" :stylist/images "https://thumb9.shutterstock.com/display_pic_with_logo/137002/402054988/stock-photo-professional-hairdresser-making-stylish-haircut-402054988.jpg"]
          [:db/add "dave" :stylist/images "https://thumb9.shutterstock.com/display_pic_with_logo/3417056/721231105/stock-photo-time-for-a-new-haircut-handsome-stylish-young-caucasian-bearded-man-came-to-barbershop-for-hairdo-721231105.jpg"]
          [:db/add "dave" :stylist/description "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Quam id leo in vitae turpis massa sed elementum. Neque sodales ut etiam sit. Massa sapien faucibus et molestie.
"]
          [:db/add "dave" :user/email "dwmartin41@gmail.com"]
          [:db/add "dave" :user/phone-number "07588361916"]
          [:db/add "dave" :user/auth0-id "dave"]
          
          [:db/add "cristina" :user/name "Cristina Arias Rey"]
          [:db/add "cristina" :location/lat 51.578820]
          [:db/add "cristina" :location/lng -0.098786]
          [:db/add "cristina" :location/address "1 Rutland Gardens, Harringey, London, N4 1JN"]
          [:db/add "cristina" :location/outcode "N4"]
          [:db/add "cristina" :location/postcode "N4 1JN"]
          [:db/add "cristina" :user/avatar "https://scontent-lhr3-1.xx.fbcdn.net/v/t1.0-0/p206x206/11205533_10152971427737309_1862020919978473913_n.jpg?oh=8302f0dd64f108c3986fb3a5d74f1032&oe=5ABDD62B"]
          [:db/add "cristina" :stylist/headline "Not quite as good as Dave."]
          [:db/add "cristina" :stylist/description "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Quam id leo in vitae turpis massa sed elementum. Neque sodales ut etiam sit. Massa sapien faucibus et molestie.
"]
          [:db/add "cristina" :stylist/images "http://bit.ly/2Bdxndq"]
          [:db/add "cristina" :stylist/images "https://st.depositphotos.com/2021695/2634/i/950/depositphotos_26346077-stock-photo-hair-stylist-curling-woman-hair.jpg"]
          [:db/add "cristina" :user/email "cris.ariasrey@gmail.com"]
          [:db/add "cristina" :user/phone-number "077712345678"]
          [:db/add "cristina" :user/auth0-id "cristina"]

          [:db/add "a" :product/type :product-type/haircut]
          [:db/add "a" :product/cost 9.99]
          [:db/add "a" :product/stylist "dave"]
          [:db/add "b" :product/type :product-type/waxing]
          [:db/add "b" :product/cost 4.99]
          [:db/add "b" :product/stylist "dave"]
          [:db/add "c" :product/type :product-type/haircut]
          [:db/add "c" :product/cost 8.99]
          [:db/add "c" :product/stylist "cristina"]
          [:db/add "d" :product/type :product-type/nails]
          [:db/add "d" :product/cost 8.99]
          [:db/add "d" :product/stylist "cristina"]

          [:db/add "app" :appointment/time #inst "2018-01-02T12:00:00.000Z"]
          [:db/add "app" :appointment/stylist "dave"]
          [:db/add "app" :appointment/stylee "cristina"]
          [:db/add "app" :location/address "123 Fake Street, Balham, London, SW129QW"]
          [:db/add "app" :location/lat 51.429813]
          [:db/add "app" :location/lng -0.163293]
          [:db/add "app" :appointment/product-types :product-type/haircut]
          [:db/add "app" :appointment/product-types :product-type/waxing]
          [:db/add "app" :appointment/status :appointment-status/confirmed]])


(comment
  (d/transact conn txs))
